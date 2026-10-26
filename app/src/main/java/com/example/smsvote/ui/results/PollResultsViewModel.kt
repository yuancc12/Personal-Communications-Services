package com.example.smsvote.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smsvote.BuildConfig
import com.example.smsvote.data.model.Poll
import com.example.smsvote.data.repository.PollRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull

data class OptionResult(val label: String, val option: String, val count: Int, val percent: Float)

sealed class AnalysisState {
    object Idle : AnalysisState()
    object Loading : AnalysisState()
    data class Success(val text: String) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

class PollResultsViewModel(val poll: Poll) : ViewModel() {
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

    private val genAI = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

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
            runCatching { genAI.generateContent(prompt).text ?: "無法生成分析" }
                .fold(
                    onSuccess = { _analysisState.value = AnalysisState.Success(it) },
                    onFailure = { _analysisState.value = AnalysisState.Error(it.localizedMessage ?: "發生錯誤") }
                )
        }
    }

    class Factory(private val poll: Poll) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = PollResultsViewModel(poll) as T
    }
}
