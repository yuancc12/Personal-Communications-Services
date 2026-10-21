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

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val senderPhone = messages.firstOrNull()?.originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody }.trim().lines().first().trim()

        Log.d("SmsReceiver", "收到簡訊 from=$senderPhone body=$body")

        val parts = body.split(" ")
        if (parts.size < 2) { Log.d("SmsReceiver", "格式錯誤，忽略"); return }
        val pollPrefix = parts[0].uppercase()

        // Extract all choice letters (supports "ea166d AB" and "ea166d A B")
        val choiceLetters = parts.drop(1).joinToString("").uppercase()
            .filter { it in 'A'..'Z' }
        if (choiceLetters.isEmpty()) { Log.d("SmsReceiver", "無有效選項"); return }

        val db = FirebaseFirestore.getInstance()
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("polls").get().await()
                val pollDoc = snapshot.documents.firstOrNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    id.take(6).uppercase() == pollPrefix
                }

                if (pollDoc == null) {
                    Log.d("SmsReceiver", "找不到投票 prefix=$pollPrefix")
                    return@launch
                }

                val pollId = pollDoc.getString("id") ?: pollDoc.id

                // Check end time
                val endTime = pollDoc.getLong("endTime")
                if (endTime != null && System.currentTimeMillis() > endTime) {
                    Log.d("SmsReceiver", "投票已截止")
                    sendReply(context, senderPhone, "此投票已截止，感謝您的參與。")
                    return@launch
                }

                // Validate choices against this poll's actual options
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
                    return@launch
                }
                if (choices.size > maxChoices) {
                    sendReply(context, senderPhone, "選項超過上限（最多選 $maxChoices 個），請重新發送。")
                    return@launch
                }

                val voterHash = sha256("$senderPhone${pollId}SMSVote2024")
                Log.d("SmsReceiver", "pollId=$pollId voterHash=$voterHash choices=$choices")

                val voteRef = db.collection("polls").document(pollId)
                    .collection("votes").document(voterHash)
                val voteData = mapOf(
                    "pollId" to pollId,
                    "voterHash" to voterHash,
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
                    return@launch
                }

                if (isDuplicate) {
                    Log.d("SmsReceiver", "重複投票，忽略")
                    sendReply(context, senderPhone, "您已投過此票，無法重複投票。")
                    return@launch
                }

                Log.d("SmsReceiver", "投票成功！choices=$choices")
                val chosenTexts = choices.joinToString("、") { c ->
                    val text = pollOptions.getOrNull(c[0] - 'A') ?: ""
                    "$c（$text）"
                }
                sendReply(context, senderPhone, "✓ 已收到您的投票，選擇：$chosenTexts")
            } catch (e: Exception) {
                Log.e("SmsReceiver", "投票失敗", e)
            } finally {
                pendingResult.finish()
            }
        }
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
