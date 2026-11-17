package com.example.smsvote.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class SmsReceiver : BroadcastReceiver() {

    companion object {
        // Deduplicate: same sender+body within 10 seconds = same SMS delivered twice
        private val recentMessages = mutableMapOf<String, Long>()
        private const val DEDUP_WINDOW_MS = 10_000L
        private const val OTP_VALID_MS = 5 * 60 * 1_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val senderPhone = messages.firstOrNull()?.originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody }.trim().lines().first().trim()

        Log.d("SmsReceiver", "收到簡訊 from=$senderPhone body=$body")

        val dedupKey = "$senderPhone|$body"
        val now = System.currentTimeMillis()
        synchronized(recentMessages) {
            recentMessages.entries.removeIf { now - it.value > DEDUP_WINDOW_MS }
            if (recentMessages.containsKey(dedupKey)) {
                Log.d("SmsReceiver", "重複訊息，忽略")
                return
            }
            recentMessages[dedupKey] = now
        }

        val db = FirebaseFirestore.getInstance()
        val pendingResult = goAsync()
        val phoneHash = sha256(senderPhone)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // OTP reply: exactly 6 digits
                if (body.matches(Regex("\\d{6}"))) {
                    handleOtpReply(context, db, senderPhone, phoneHash, body)
                    return@launch
                }

                val parts = body.split(" ")
                if (parts.size < 2) { Log.d("SmsReceiver", "格式錯誤，忽略"); return@launch }
                val pollPrefix = parts[0].uppercase()
                val choiceLetters = parts.drop(1).joinToString("").uppercase()
                    .filter { it in 'A'..'Z' }
                if (choiceLetters.isEmpty()) { Log.d("SmsReceiver", "無有效選項"); return@launch }

                // OTP gate: unverified senders must verify before vote is counted
                val verified = db.collection("otpVerified").document(phoneHash).get().await()
                if (!verified.exists()) {
                    issuePendingVote(context, db, senderPhone, phoneHash, body)
                    return@launch
                }

                processVote(context, db, senderPhone, pollPrefix, choiceLetters)
            } catch (e: Exception) {
                Log.e("SmsReceiver", "處理失敗", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleOtpReply(
        context: Context,
        db: FirebaseFirestore,
        senderPhone: String,
        phoneHash: String,
        otp: String,
    ) {
        val pendingDoc = db.collection("otpPending").document(phoneHash).get().await()
        if (!pendingDoc.exists()) {
            sendReply(context, senderPhone, "找不到待驗證的投票，請重新發送您的投票簡訊。")
            return
        }

        val expiresAt = pendingDoc.getLong("expiresAt") ?: 0L
        if (System.currentTimeMillis() > expiresAt) {
            db.collection("otpPending").document(phoneHash).delete().await()
            sendReply(context, senderPhone, "驗證碼已過期，請重新發送您的投票簡訊。")
            return
        }

        if (otp != pendingDoc.getString("otp")) {
            sendReply(context, senderPhone, "驗證碼錯誤，請再試一次。")
            return
        }

        // Mark phone as permanently verified
        db.collection("otpVerified").document(phoneHash)
            .set(mapOf("verifiedAt" to System.currentTimeMillis())).await()

        val pendingBody = pendingDoc.getString("pendingBody") ?: ""
        db.collection("otpPending").document(phoneHash).delete().await()

        if (pendingBody.isNotEmpty()) {
            val parts = pendingBody.split(" ")
            val pollPrefix = parts[0].uppercase()
            val choiceLetters = parts.drop(1).joinToString("").uppercase().filter { it in 'A'..'Z' }
            processVote(context, db, senderPhone, pollPrefix, choiceLetters)
        }
    }

    private suspend fun issuePendingVote(
        context: Context,
        db: FirebaseFirestore,
        senderPhone: String,
        phoneHash: String,
        body: String,
    ) {
        val otp = (100000..999999).random().toString()
        db.collection("otpPending").document(phoneHash).set(
            mapOf(
                "otp" to otp,
                "expiresAt" to System.currentTimeMillis() + OTP_VALID_MS,
                "pendingBody" to body,
            )
        ).await()
        sendReply(context, senderPhone, "驗證碼：$otp\n請在 5 分鐘內回覆此驗證碼以完成投票。")
        Log.d("SmsReceiver", "已發送OTP給 $senderPhone")
    }

    private suspend fun processVote(
        context: Context,
        db: FirebaseFirestore,
        senderPhone: String,
        pollPrefix: String,
        choiceLetters: String,
    ) {
        val snapshot = db.collection("polls").get().await()
        val pollDoc = snapshot.documents.firstOrNull { doc ->
            val id = doc.getString("id") ?: doc.id
            id.take(6).uppercase() == pollPrefix
        }

        if (pollDoc == null) {
            Log.d("SmsReceiver", "找不到投票 prefix=$pollPrefix")
            return
        }

        val pollId = pollDoc.getString("id") ?: pollDoc.id

        val endTime = pollDoc.getLong("endTime")
        if (endTime != null && System.currentTimeMillis() > endTime) {
            Log.d("SmsReceiver", "投票已截止")
            sendReply(context, senderPhone, "此投票已截止，感謝您的參與。")
            return
        }

        @Suppress("UNCHECKED_CAST")
        val pollOptions = (pollDoc.get("options") as? List<String>)?.filter { it.isNotEmpty() }
            ?: listOfNotNull(
                pollDoc.getString("optionA"),
                pollDoc.getString("optionB"),
                pollDoc.getString("optionC")
            ).filter { it.isNotEmpty() }
        val validChoices = pollOptions.indices.map { ('A' + it).toString() }
        val maxChoices = (pollDoc.getLong("maxChoices") ?: 1L).toInt()

        val choices = choiceLetters.map { it.toString() }
            .filter { it in validChoices }
            .distinct()

        if (choices.isEmpty()) {
            Log.d("SmsReceiver", "選項無效: $choiceLetters (有效: $validChoices)")
            sendReply(context, senderPhone, "選項無效，可選：${validChoices.joinToString("/")}。")
            return
        }
        if (choices.size > maxChoices) {
            sendReply(context, senderPhone, "選項超過上限（最多選 $maxChoices 個），請重新發送。")
            return
        }

        val voterHash = sha256("$senderPhone${pollId}SMSVote2024")
        Log.d("SmsReceiver", "pollId=$pollId voterHash=$voterHash choices=$choices")

        val recipientSnap = db.collection("recipients")
            .whereEqualTo("phoneNumber", senderPhone)
            .get().await()
        val recipientId = recipientSnap.documents.firstOrNull()?.id ?: ""

        val voteRef = db.collection("polls").document(pollId)
            .collection("votes").document(voterHash)
        val voteData = mapOf(
            "pollId" to pollId,
            "voterHash" to voterHash,
            "recipientId" to recipientId,
            "choices" to choices,
            "timestamp" to System.currentTimeMillis()
        )

        // Transaction: check + write are atomic — prevents duplicate votes from concurrent SMS
        var isDuplicate = false
        try {
            db.runTransaction { transaction ->
                val snap = transaction.get(voteRef)
                if (snap.exists()) { isDuplicate = true; return@runTransaction }
                transaction.set(voteRef, voteData)
            }.await()
        } catch (e: FirebaseFirestoreException) {
            Log.e("SmsReceiver", "Transaction failed", e)
            return
        }

        if (isDuplicate) {
            Log.d("SmsReceiver", "重複投票，忽略")
            sendReply(context, senderPhone, "您已投過此票，無法重複投票。")
            return
        }

        Log.d("SmsReceiver", "投票成功！choices=$choices")
        val chosenTexts = choices.joinToString("、") { c ->
            val text = pollOptions.getOrNull(c[0] - 'A') ?: ""
            "$c（$text）"
        }
        sendReply(context, senderPhone, "✓ 已收到您的投票，選擇：$chosenTexts")
    }

    private fun sendReply(context: Context, phone: String, message: String) {
        try {
            context.getSystemService(SmsManager::class.java)
                .sendTextMessage(phone, null, message, null, null)
            Log.d("SmsReceiver", "已回覆: $message")
        } catch (e: Exception) {
            Log.e("SmsReceiver", "回覆失敗", e)
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
