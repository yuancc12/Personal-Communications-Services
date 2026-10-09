package com.example.personal_communications_services.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.personal_communications_services.data.AppPreferences
import com.example.personal_communications_services.data.DetectionRecordEntity
import com.example.personal_communications_services.data.LocalStorage
import com.example.personal_communications_services.engine.RuleEngine
import com.example.personal_communications_services.llm.LlmClassifier
import com.example.personal_communications_services.model.DetectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DetectViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = LocalStorage.getInstance(app)
    private val prefs   = AppPreferences(app)

    private val _result    = MutableStateFlow<DetectionResult?>(null)
    val result: StateFlow<DetectionResult?> = _result.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun analyze(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true

            val ruleResult = RuleEngine.analyze(message)
            val llmEnabled = prefs.llmEnabled.first()

            val final = if (llmEnabled && ruleResult.score in 31..70) {
                try { LlmClassifier().classify(message, ruleResult) }
                catch (_: Exception) { ruleResult }
            } else ruleResult

            _result.value = final
            _isLoading.value = false

            val saveFullBody = prefs.saveFullBody.first()
            storage.insert(
                DetectionRecordEntity(
                    riskLevel    = final.riskLevel.name,
                    score        = final.score,
                    summary      = if (saveFullBody) message.take(50) else "${message.take(30)}…",
                    reasons      = final.reasons.joinToString("|"),
                    senderNumber = "手動輸入",
                    timestamp    = System.currentTimeMillis(),
                    source       = final.source.name
                )
            )
        }
    }
}
