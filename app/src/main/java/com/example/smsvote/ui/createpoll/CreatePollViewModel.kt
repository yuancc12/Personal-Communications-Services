package com.example.smsvote.ui.createpoll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsvote.data.model.Poll
import com.example.smsvote.data.repository.PollRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class CreatePollUiState(
    val question: String = "",
    val options: List<String> = listOf("", ""),
    val maxChoices: Int = 1,
    val endTime: Long? = null,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null
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
        val endOfDay = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
        it.copy(endTime = endOfDay)
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
                repository.insertPoll(
                    Poll(
                        question = state.question,
                        options = state.options,
                        maxChoices = state.maxChoices,
                        endTime = state.endTime,
                        createdBy = uid
                    )
                )
                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "儲存失敗：${e.localizedMessage}") }
            }
        }
    }
}
