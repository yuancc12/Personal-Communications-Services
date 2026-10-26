package com.example.smsvote.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsvote.data.repository.InvitationRecord
import com.example.smsvote.data.repository.InvitationRepository
import com.example.smsvote.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PreApproveUiState(
    val email: String = "",
    val role: String = "operator",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false
)

class RoleManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserRepository()
    private val inviteRepository = InvitationRepository()
    val currentUid: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val users: StateFlow<List<com.example.smsvote.data.model.AppUser>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserRole: String
        get() = users.value.find { it.uid == currentUid }?.role ?: "operator"

    val preApprovedList: StateFlow<List<Pair<String, String>>> = repository.getPreApproved()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invitations: StateFlow<List<InvitationRecord>> = inviteRepository.getInvitations(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _preApprove = MutableStateFlow(PreApproveUiState())
    val preApprove: StateFlow<PreApproveUiState> = _preApprove.asStateFlow()

    fun onPreApproveEmailChange(email: String) {
        _preApprove.value = _preApprove.value.copy(email = email, errorMessage = null, successMessage = null)
    }

    fun onPreApproveRoleChange(role: String) {
        _preApprove.value = _preApprove.value.copy(role = role)
    }

    fun submitPreApprove() {
        val email = _preApprove.value.email.trim()
        if (email.isBlank()) {
            _preApprove.value = _preApprove.value.copy(errorMessage = "請輸入 Email")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _preApprove.value = _preApprove.value.copy(errorMessage = "Email 格式不正確")
            return
        }
        viewModelScope.launch {
            _preApprove.value = _preApprove.value.copy(isLoading = true, errorMessage = null, successMessage = null)
            runCatching { repository.preApproveEmail(email, _preApprove.value.role) }
                .onSuccess { emailError ->
                    if (emailError == null) {
                        _preApprove.value = PreApproveUiState(successMessage = "✓ 已新增並發送邀請信給 $email")
                    } else {
                        _preApprove.value = PreApproveUiState(successMessage = "✓ 已新增授權，但寄信失敗：$emailError")
                    }
                }
                .onFailure {
                    _preApprove.value = _preApprove.value.copy(
                        isLoading = false,
                        errorMessage = "新增失敗：${it.localizedMessage}"
                    )
                }
        }
    }

    fun removePreApproved(email: String) {
        viewModelScope.launch { repository.removePreApproved(email) }
    }

    fun setAdmin(uid: String) {
        viewModelScope.launch { repository.updateRole(uid, "admin") }
    }

    fun setOperator(uid: String) {
        viewModelScope.launch { repository.updateRole(uid, "operator") }
    }

    fun approveUser(uid: String) {
        viewModelScope.launch { repository.approveUser(uid) }
    }

    fun rejectUser(uid: String) {
        viewModelScope.launch { repository.rejectUser(uid) }
    }
}
