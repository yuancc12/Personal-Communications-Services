package com.example.personal_communications_services.llm

import com.example.personal_communications_services.BuildConfig
import com.example.personal_communications_services.engine.ScamClassifier
import com.example.personal_communications_services.model.DetectionResult
import com.example.personal_communications_services.model.ResultSource
import com.example.personal_communications_services.model.RiskLevel
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 使用 Anthropic Claude API 做二次判斷。
 * 只在規則分數落於 31-70 灰色地帶才被呼叫。
 * API key 從 BuildConfig 讀取（源自 local.properties），不寫死進程式。
 * 呼叫前一律透過 Deidentifier 去識別化。
 * 失敗或無網路時自動 fallback 回規則結果。
 */
class LlmClassifier : ScamClassifier {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    override suspend fun classify(message: String): DetectionResult {
        val ruleResult = DetectionResult(score = 0, riskLevel = RiskLevel.SUSPICIOUS, reasons = emptyList())
        val apiKey = BuildConfig.ANTHROPIC_API_KEY
        if (apiKey.isBlank()) return ruleResult

        return withContext(Dispatchers.IO) {
            try {
                val sanitized = Deidentifier.deidentify(message)
                val prompt = buildPrompt(sanitized, ruleResult)
                val responseJson = callApi(apiKey, prompt)
                parseResponse(responseJson, ruleResult)
            } catch (_: Exception) {
                // 任何錯誤（無網路、逾時、解析失敗）→ fallback
                ruleResult
            }
        }
    }

    private fun buildPrompt(sanitized: String, ruleResult: DetectionResult): String = """
        你是詐騙簡訊分析專家。請分析下列已去識別化的簡訊，判斷詐騙風險。

        簡訊內容：
        $sanitized

        規則引擎初步評分：${ruleResult.score}/100
        規則命中原因：${ruleResult.reasons.joinToString("；")}

        請僅輸出 JSON，不加任何其他文字：
        {"risk":"safe|suspicious|high","reason":"繁體中文說明（30字以內）"}
    """.trimIndent()

    private fun callApi(apiKey: String, prompt: String): String {
        val body = mapOf(
            "model" to "claude-haiku-4-5-20251001",
            "max_tokens" to 150,
            "messages" to listOf(mapOf("role" to "user", "content" to prompt))
        )
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("API 錯誤 ${resp.code}")
            return resp.body?.string() ?: throw Exception("空回應")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseResponse(json: String, fallback: DetectionResult): DetectionResult {
        return try {
            val root = gson.fromJson(json, Map::class.java)
            val text = (root["content"] as? List<Map<String, Any>>)
                ?.firstOrNull()?.get("text") as? String ?: return fallback
            val jsonStr = Regex("""\{[^}]+\}""").find(text)?.value ?: return fallback
            val llm = gson.fromJson(jsonStr, LlmResponse::class.java)
            val level = when (llm.risk?.lowercase()) {
                "safe"       -> RiskLevel.SAFE
                "suspicious" -> RiskLevel.SUSPICIOUS
                "high"       -> RiskLevel.HIGH_RISK
                else         -> return fallback
            }
            fallback.copy(
                riskLevel = level,
                reasons = fallback.reasons + "🤖 AI 分析：${llm.reason.orEmpty()}",
                source = ResultSource.LLM
            )
        } catch (_: Exception) { fallback }
    }

    private data class LlmResponse(
        @SerializedName("risk") val risk: String?,
        @SerializedName("reason") val reason: String?
    )
}
