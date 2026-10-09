package com.example.personal_communications_services.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personal_communications_services.data.AppPreferences
import com.example.personal_communications_services.data.DetectionRecordEntity
import com.example.personal_communications_services.data.LocalStorage
import com.example.personal_communications_services.engine.RuleEngine
import com.example.personal_communications_services.llm.Deidentifier
import com.example.personal_communications_services.llm.LlmClassifier
import com.example.personal_communications_services.model.RiskLevel
import com.example.personal_communications_services.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPreferences(app)

    val autoMonitor = prefs.autoMonitorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val llmEnabled = prefs.llmEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val saveFullBody = prefs.saveFullBody
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val notifyParent = prefs.notifyParent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val parentNumber = prefs.parentNumber
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun setAutoMonitor(v: Boolean)  = viewModelScope.launch { prefs.setAutoMonitor(v) }
    fun setLlmEnabled(v: Boolean)   = viewModelScope.launch { prefs.setLlmEnabled(v) }
    fun setSaveFullBody(v: Boolean) = viewModelScope.launch { prefs.setSaveFullBody(v) }
    fun setNotifyParent(v: Boolean) = viewModelScope.launch { prefs.setNotifyParent(v) }
    fun setParentNumber(v: String)  = viewModelScope.launch { prefs.setParentNumber(v) }

    private val _scanStatus = MutableStateFlow("")
    val scanStatus = _scanStatus.asStateFlow()

    fun scanInbox(context: android.content.Context) = viewModelScope.launch {
        _scanStatus.value = "掃描中…"
        val storage = LocalStorage.getInstance(context)
        val llmEnabledVal = prefs.llmEnabled.first()
        val saveFullBodyVal = prefs.saveFullBody.first()

        val results = withContext(Dispatchers.IO) {
            val uri = android.net.Uri.parse("content://sms/inbox")
            val cursor = context.contentResolver.query(
                uri, arrayOf("address", "body", "date"), null, null, "date DESC"
            ) ?: return@withContext 0

            var count = 0
            cursor.use {
                val addrIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")
                while (it.moveToNext() && count < 50) {
                    val sender = it.getString(addrIdx).orEmpty()
                    val body   = it.getString(bodyIdx).orEmpty()
                    val date   = it.getLong(dateIdx)
                    if (body.isBlank()) continue

                    val ruleResult = RuleEngine.analyze(body)
                    val final = if (llmEnabledVal && ruleResult.score in 31..70) {
                        try { LlmClassifier().classify(body, ruleResult) }
                        catch (_: Exception) { ruleResult }
                    } else ruleResult

                    storage.insert(DetectionRecordEntity(
                        riskLevel    = final.riskLevel.name,
                        score        = final.score,
                        summary      = if (saveFullBodyVal) body.take(50) else "${body.take(30)}…",
                        reasons      = final.reasons.joinToString("|"),
                        senderNumber = Deidentifier.deidentify(sender),
                        timestamp    = date,
                        source       = final.source.name
                    ))
                    count++
                }
            }
            count
        }
        _scanStatus.value = "掃描完成！已分析 $results 則簡訊"
    }

    private val _simStatus = MutableStateFlow("")
    val simStatus = _simStatus.asStateFlow()

    fun simulateSms(context: android.content.Context) = viewModelScope.launch {
        _simStatus.value = "分析中…"
        val fakeSender = "0900123456"
        val fakeBody   = "【緊急通知】您的帳戶凍結，請點擊 bit.ly/verify000 立即領取並驗證，否則將永久停用。"

        val storage    = LocalStorage.getInstance(context)
        val ruleResult = RuleEngine.analyze(fakeBody)
        val llmEnabled = prefs.llmEnabled.first()
        val afterLlm   = if (llmEnabled && ruleResult.score in 31..70) {
            try { LlmClassifier().classify(fakeBody, ruleResult) }
            catch (_: Exception) { ruleResult }
        } else ruleResult

        val blacklisted = storage.isBlacklisted(fakeSender)
        val final = if (blacklisted) afterLlm.copy(
            score     = 100,
            riskLevel = RiskLevel.HIGH_RISK,
            reasons   = listOf("📵 號碼在黑名單中") + afterLlm.reasons
        ) else afterLlm

        val saveFullBody = prefs.saveFullBody.first()
        storage.insert(DetectionRecordEntity(
            riskLevel    = final.riskLevel.name,
            score        = final.score,
            summary      = if (saveFullBody) fakeBody.take(50) else "${fakeBody.take(30)}…",
            reasons      = final.reasons.joinToString("|"),
            senderNumber = Deidentifier.deidentify(fakeSender),
            timestamp    = System.currentTimeMillis(),
            source       = final.source.name
        ))

        if (final.riskLevel == RiskLevel.HIGH_RISK) {
            NotificationHelper.sendHighRiskAlert(
                context,
                "${Deidentifier.deidentify(fakeSender)}：${fakeBody.take(30)}",
                System.currentTimeMillis().toInt()
            )
            val notifyParent = prefs.notifyParent.first()
            if (notifyParent) {
                withContext(Dispatchers.IO) {
                    try {
                        val url  = java.net.URL("http://10.0.2.2:8765/alert")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.doOutput      = true
                        conn.connectTimeout = 3000
                        conn.readTimeout    = 3000
                        val payload = "sender=${java.net.URLEncoder.encode(Deidentifier.deidentify(fakeSender), "UTF-8")}" +
                                      "&message=${java.net.URLEncoder.encode(fakeBody.take(40), "UTF-8")}"
                        conn.outputStream.write(payload.toByteArray())
                        conn.outputStream.flush()
                        conn.responseCode
                        conn.disconnect()
                    } catch (_: Exception) {}
                }
            }
        }

        _simStatus.value = "完成！風險：${final.riskLevel.label}（${final.score}分）"
    }
}
