package com.example.personal_communications_services.llm

import com.example.personal_communications_services.BuildConfig
import com.example.personal_communications_services.engine.ScamClassifier
import com.example.personal_communications_services.model.DetectionResult
import com.example.personal_communications_services.model.ResultSource
import com.example.personal_communications_services.model.RiskLevel
import com.example.personal_communications_services.model.ScamType
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 使用 TypeSafe JEV API 判斷簡訊詐騙風險與類型。
 * 單次呼叫同時詢問 risk_level 和 scam_type 兩個問題（JEV 平行處理）。
 * API key 從 BuildConfig 讀取（源自 local.properties），不寫死進程式。
 * 呼叫前透過 Deidentifier 去識別化，確保個資不離開裝置。
 * 失敗或無網路時回傳 SUSPICIOUS 保守預設值。
 */
class JevClassifier private constructor() : ScamClassifier {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    companion object {
        val instance: JevClassifier by lazy { JevClassifier() }
    }

    override suspend fun classify(message: String): DetectionResult {
        val apiKey = BuildConfig.TYPESAFE_API_KEY
        if (apiKey.isBlank()) return fallbackResult("未設定 TYPESAFE_API_KEY")

        return withContext(Dispatchers.IO) {
            try {
                val sanitized = Deidentifier.deidentify(message)
                val responseJson = callApi(apiKey, sanitized)
                parseResponse(responseJson)
            } catch (e: Exception) {
                fallbackResult("JEV 呼叫失敗：${e.message?.take(40)}")
            }
        }
    }

    private fun callApi(apiKey: String, sanitized: String): String {
        val body = mapOf(
            "state" to sanitized,
            "model" to "jev-latest",
            "questions" to mapOf(
                "risk_level" to mapOf(
                    "type" to "choice",
                    "instructions" to "Classify this SMS message's scam risk level in the context of Taiwanese mobile fraud",
                    "criteria" to mapOf(
                        "safe"       to "Legitimate message with no scam indicators",
                        "suspicious" to "Some scam-like patterns but not conclusive",
                        "high"       to "Clear scam or phishing indicators, likely fraudulent"
                    )
                ),
                "scam_type" to mapOf(
                    "type" to "choice",
                    "instructions" to "What category of scam is this SMS message?",
                    "criteria" to mapOf(
                        "package"  to "Fake package delivery, logistics issue, or customs fee",
                        "lottery"  to "Fake prize, lottery win, or reward claim",
                        "financial" to "Bank account freeze, credit card fraud, or payment request",
                        "identity" to "Fake account verification, OTP request, or identity check",
                        "none"     to "Not a scam or type cannot be determined"
                    )
                )
            )
        )
        val request = Request.Builder()
            .url("https://api.typesafe.ai/v1/systemone")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
            return resp.body?.string() ?: throw Exception("空回應")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseResponse(json: String): DetectionResult {
        val root = gson.fromJson(json, Map::class.java)
        val answers = root["answers"] as? Map<String, Any>
            ?: return fallbackResult("無法解析 answers")

        // 解析風險等級
        val riskAnswer = answers["risk_level"] as? Map<String, Any>
            ?: return fallbackResult("無法解析 risk_level")
        val riskChoice = riskAnswer["choice"] as? String
            ?: return fallbackResult("無法解析 choice")
        val confidence = riskAnswer["confidence"] as? Double

        val level = when (riskChoice.lowercase()) {
            "safe"       -> RiskLevel.SAFE
            "suspicious" -> RiskLevel.SUSPICIOUS
            "high"       -> RiskLevel.HIGH_RISK
            else         -> return fallbackResult("未知分類：$riskChoice")
        }

        // 解析詐騙類型
        val typeAnswer = answers["scam_type"] as? Map<String, Any>
        val typeChoice = typeAnswer?.get("choice") as? String
        val scamType = when (typeChoice?.lowercase()) {
            "package"   -> ScamType.PACKAGE
            "lottery"   -> ScamType.LOTTERY
            "financial" -> ScamType.FINANCIAL
            "identity"  -> ScamType.IDENTITY
            else        -> ScamType.NONE
        }

        val score = when (level) {
            RiskLevel.SAFE       -> 10
            RiskLevel.SUSPICIOUS -> 50
            RiskLevel.HIGH_RISK  -> 85
        }
        val confidenceLabel = confidence?.let {
            "🤖 JEV 信心度：${"%.0f".format(it * 100)}%"
        } ?: "🤖 JEV 已分析"

        return DetectionResult(
            score      = score,
            riskLevel  = level,
            reasons    = listOf(confidenceLabel),
            source     = ResultSource.LLM,
            confidence = confidence,
            scamType   = scamType
        )
    }

    private fun fallbackResult(reason: String) = DetectionResult(
        score      = 50,
        riskLevel  = RiskLevel.SUSPICIOUS,
        reasons    = listOf("⚠️ $reason，請謹慎判斷"),
        source     = ResultSource.RULE,
        confidence = null,
        scamType   = null
    )
}
