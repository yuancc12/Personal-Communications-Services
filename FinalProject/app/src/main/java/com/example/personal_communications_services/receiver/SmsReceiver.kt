package com.example.personal_communications_services.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.personal_communications_services.data.AppPreferences
import com.example.personal_communications_services.data.DetectionRecordEntity
import com.example.personal_communications_services.data.LocalStorage
import com.example.personal_communications_services.llm.Deidentifier
import com.example.personal_communications_services.llm.JevClassifier
import com.example.personal_communications_services.model.RiskLevel
import com.example.personal_communications_services.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "onReceive 被呼叫，action=${intent.action}")
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        Log.d("SmsReceiver", "收到簡訊廣播，開始分析")

        val msgs   = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val sender = msgs.firstOrNull()?.originatingAddress.orEmpty()
        val body   = msgs.joinToString("") { it.messageBody }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs   = AppPreferences(context)
                val autoOn  = prefs.autoMonitorEnabled.first()
                Log.d("SmsReceiver", "autoMonitor=$autoOn")
                if (!autoOn) return@launch

                val storage     = LocalStorage.getInstance(context)
                val blacklisted = storage.isBlacklisted(sender)

                val afterJev = JevClassifier.instance.classify(body)

                val final = if (blacklisted) afterJev.copy(
                    score     = 100,
                    riskLevel = RiskLevel.HIGH_RISK,
                    reasons   = listOf("📵 號碼在黑名單中") + afterJev.reasons
                ) else afterJev

                Log.d("SmsReceiver", "分析完成 riskLevel=${final.riskLevel} score=${final.score}")

                val saveFullBody = prefs.saveFullBody.first()
                storage.insert(
                    DetectionRecordEntity(
                        riskLevel    = final.riskLevel.name,
                        score        = final.score,
                        summary      = if (saveFullBody) body.take(50) else "${body.take(30)}…",
                        reasons      = final.reasons.joinToString("|"),
                        senderNumber = Deidentifier.deidentify(sender),
                        timestamp    = System.currentTimeMillis(),
                        source       = final.source.name,
                        scamType     = final.scamType?.name ?: ""
                    )
                )

                if (final.riskLevel == RiskLevel.HIGH_RISK) {
                    NotificationHelper.sendHighRiskAlert(
                        context,
                        "${Deidentifier.deidentify(sender)}：${body.take(30)}",
                        System.currentTimeMillis().toInt()
                    )
                    Log.i("PARENT_ALERT", "HIGH_RISK|${Deidentifier.deidentify(sender)}|${body.take(40)}")
                    // 通知家長（呼叫 Windows 橋接服務 → adb emu sms send 注入到家長模擬器）
                    val notifyParent = prefs.notifyParent.first()
                    if (notifyParent) {
                        try {
                            val url  = java.net.URL("http://10.0.2.2:9876/alert")
                            val conn = url.openConnection() as java.net.HttpURLConnection
                            conn.requestMethod = "POST"
                            conn.doOutput      = true
                            conn.connectTimeout = 3000
                            conn.readTimeout    = 3000
                            val payload = "sender=${java.net.URLEncoder.encode(Deidentifier.deidentify(sender), "UTF-8")}" +
                                          "&message=${java.net.URLEncoder.encode(body.take(40), "UTF-8")}"
                            conn.outputStream.write(payload.toByteArray())
                            conn.outputStream.flush()
                            val code = conn.responseCode
                            Log.i("PARENT_ALERT", "橋接服務回應 HTTP $code")
                            conn.disconnect()
                        } catch (e: Exception) {
                            Log.w("PARENT_ALERT", "橋接服務呼叫失敗：${e.message}")
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
