package com.example.smsvote.ui.recipients

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsvote.data.model.Recipient
import com.example.smsvote.data.repository.RecipientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipientsUiState(
    val newName: String = "",
    val newPhone: String = "",
    val errorMessage: String? = null
)

class RecipientsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RecipientRepository()

    val recipients: StateFlow<List<Recipient>> = repository.getRecipients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(RecipientsUiState())
    val uiState: StateFlow<RecipientsUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(newName = value) }
    fun onPhoneChange(value: String) = _uiState.update { it.copy(newPhone = value) }

    fun addRecipient() {
        val state = _uiState.value
        if (state.newName.isBlank() || state.newPhone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "請填寫姓名和電話") }
            return
        }
        viewModelScope.launch {
            try {
                repository.addRecipient(Recipient(name = state.newName, phoneNumber = state.newPhone))
                _uiState.update { it.copy(newName = "", newPhone = "", errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "新增失敗：${e.localizedMessage}") }
            }
        }
    }

    fun deleteRecipient(id: String) {
        viewModelScope.launch { repository.deleteRecipient(id) }
    }
}
