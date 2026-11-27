package com.example.smsvote.ui.createpoll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsvote.BuildConfig
import com.example.smsvote.data.model.Poll
import com.example.smsvote.data.repository.PollRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class CreatePollUiState(
    val question: String = "",
    val options: List<String> = listOf("", ""),
    val maxChoices: Int = 1,
    val endTime: Long? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null,
    val category: String = ""
)

class CreatePollViewModel : ViewModel() {
    private val repository = PollRepository()

    private val _uiState = MutableStateFlow(CreatePollUiState())
    val uiState: StateFlow<CreatePollUiState> = _uiState.asStateFlow()

    fun onQuestionChange(value: String) = _uiState.update { it.copy(question = value) }

    fun onOptionChange(index: Int, value: String) = _uiState.update {
        val updated = it.options.toMutableList().also { list -> list[index] = value }
        it.copy(options = updated)
    }

    fun addOption() = _uiState.update {
        if (it.options.size >= 5) it else it.copy(options = it.options + "")
    }

    fun removeOption(index: Int) = _uiState.update {
        if (it.options.size <= 2) it
        else {
            val newOptions = it.options.toMutableList().also { list -> list.removeAt(index) }
            it.copy(options = newOptions, maxChoices = it.maxChoices.coerceAtMost(newOptions.size))
        }
    }

    fun increaseMaxChoices() = _uiState.update {
        it.copy(maxChoices = (it.maxChoices + 1).coerceAtMost(it.options.size))
    }

    fun decreaseMaxChoices() = _uiState.update {
        it.copy(maxChoices = (it.maxChoices - 1).coerceAtLeast(1))
    }

    fun toggleEndTime(enabled: Boolean) = _uiState.update {
        val endTime = if (enabled) {
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis
        } else null
        it.copy(endTime = endTime)
    }

    fun onEndTimeChange(dateMillis: Long) = _uiState.update {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        if (dateMillis < today) return@update it.copy(errorMessage = "截止時間不能早於今天")
        val endOfDay = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }.timeInMillis
        it.copy(endTime = endOfDay, errorMessage = null)
    }

    fun savePoll() {
        val state = _uiState.value
        if (state.question.isBlank() || state.options.any { it.isBlank() }) {
            _uiState.update { it.copy(errorMessage = "請填寫所有欄位") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val category = try {
                    classifyWithJev(state.question, state.options)
                } catch (e: Exception) { "其他" }
                _uiState.update { it.copy(category = category) }
                repository.insertPoll(
                    Poll(
                        question = state.question,
                        options = state.options,
                        maxChoices = state.maxChoices,
                        endTime = state.endTime,
                        createdBy = uid,
                        category = category
                    )
                )
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "儲存失敗：${e.localizedMessage}") }
            }
        }
    }

    private suspend fun classifyWithJev(question: String, options: List<String>): String =
        withContext(Dispatchers.IO) {
            val url = java.net.URL("https://api.typesafe.ai/v1/systemone")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.JEV_API_KEY}")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            val body = org.json.JSONObject().apply {
                put("state", "投票問題：$question\n選項：${options.joinToString("、")}")
                put("model", "jev-latest")
                put("questions", org.json.JSONObject().apply {
                    put("category", org.json.JSONObject().apply {
                        put("type", "choice")
                        put("instructions", "這個投票屬於哪個類別？")
                        put("criteria", org.json.JSONObject().apply {
                            put("政治", "政治相關、選舉、政府政策、公共事務")
                            put("美食", "食物、餐廳、料理、飲食偏好")
                            put("娛樂", "電影、音樂、遊戲、娛樂活動")
                            put("體育", "運動、競技、球賽、健身")
                            put("工作", "職場、工作、商業、職業")
                            put("教育", "學習、學校、課程、教育")
                            put("健康", "健康、醫療、養生、身體")
                            put("生活", "日常生活、生活習慣、居家")
                            put("科技", "科技、電腦、手機、網路")
                            put("其他", "不屬於以上任何類別")
                        })
                    })
                })
            }.toString()
            conn.outputStream.write(body.toByteArray())
            if (conn.responseCode != 200) return@withContext "其他"
            val response = conn.inputStream.bufferedReader().readText()
            org.json.JSONObject(response)
                .getJSONObject("answers")
                .getJSONObject("category")
                .getString("choice")
        }
}
