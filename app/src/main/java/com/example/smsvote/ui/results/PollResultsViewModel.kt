package com.example.smsvote.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smsvote.BuildConfig
import com.example.smsvote.data.model.Poll
import com.example.smsvote.data.repository.PollRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class OptionResult(val label: String, val option: String, val count: Int, val percent: Float)

sealed class AnalysisState {
    object Idle : AnalysisState()
    object Loading : AnalysisState()
    data class Success(val text: String) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

class PollResultsViewModel(val poll: Poll, private val aiProvider: String, private val aiApiKey: String) : ViewModel() {
    private val repo = PollRepository()

    private val votesFlow = repo.getVotesFlow(poll.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val results: StateFlow<List<OptionResult>> = votesFlow.map { votes ->
        val total = votes.size
        poll.options.mapIndexed { i, option ->
            val letter = ('A' + i).toString()
            val count = votes.count { letter in it.choices }
            OptionResult(
                label = letter,
                option = option,
                count = count,
                percent = if (total == 0) 0f else count.toFloat() / total
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalVotes: StateFlow<Int> = votesFlow.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sentCount: StateFlow<Int> = repo.getPollByIdFlow(poll.id)
        .filterNotNull()
        .map { it.sentRecipientIds.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), poll.sentRecipientIds.size)

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    fun generateAnalysis() {
        val currentResults = results.value
        val total = totalVotes.value
        if (_analysisState.value is AnalysisState.Loading) return

        viewModelScope.launch {
            _analysisState.value = AnalysisState.Loading
            val prompt = buildString {
                appendLine("你是一個投票結果分析師，請用繁體中文分析以下投票結果，回覆在 200 字以內，語氣簡潔專業：")
                appendLine()
                appendLine("投票主題：${poll.question}")
                if (poll.maxChoices > 1) appendLine("投票方式：複選（最多選 ${poll.maxChoices} 個）")
                appendLine("總票數：$total 票")
                appendLine()
                appendLine("各選項得票：")
                currentResults.forEach { r ->
                    appendLine("${r.label}. ${r.option}：${r.count} 票（${"%.1f".format(r.percent * 100)}%）")
                }
                appendLine()
                append("請依序提供：① 結果摘要 ② 主要趨勢 ③ 一句建議")
            }
            val effectiveKey = aiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }
            runCatching {
                when (aiProvider) {
                    "openai" -> callOpenAI(effectiveKey, prompt)
                    "claude" -> callClaude(effectiveKey, prompt)
                    else     -> callGemini(effectiveKey, prompt)
                }
            }.fold(
                onSuccess = { _analysisState.value = AnalysisState.Success(it) },
                onFailure = { _analysisState.value = AnalysisState.Error(it.localizedMessage ?: "發生錯誤") }
            )
        }
    }

    private suspend fun callGemini(key: String, prompt: String): String {
        val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = key)
        return model.generateContent(prompt).text ?: "無法生成分析"
    }

    private suspend fun callOpenAI(key: String, prompt: String): String = withContext(Dispatchers.IO) {
        val url = java.net.URL("https://api.openai.com/v1/chat/completions")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $key")
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.doOutput = true
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("max_tokens", 600)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        }.toString()
        conn.outputStream.use { it.write(body.toByteArray()) }
        val code = conn.responseCode
        if (code != 200) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            throw RuntimeException("OpenAI HTTP $code: $err")
        }
        val resp = JSONObject(conn.inputStream.bufferedReader().readText())
        resp.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
    }

    private suspend fun callClaude(key: String, prompt: String): String = withContext(Dispatchers.IO) {
        val url = java.net.URL("https://api.anthropic.com/v1/messages")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-api-key", key)
        conn.setRequestProperty("anthropic-version", "2023-06-01")
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.doOutput = true
        val body = JSONObject().apply {
            put("model", "claude-haiku-4-5-20251001")
            put("max_tokens", 600)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        }.toString()
        conn.outputStream.use { it.write(body.toByteArray()) }
        val code = conn.responseCode
        if (code != 200) {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            throw RuntimeException("Claude HTTP $code: $err")
        }
        val resp = JSONObject(conn.inputStream.bufferedReader().readText())
        resp.getJSONArray("content").getJSONObject(0).getString("text")
    }

    class Factory(private val poll: Poll, private val aiProvider: String, private val aiApiKey: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = PollResultsViewModel(poll, aiProvider, aiApiKey) as T
    }
}
