package com.example.smsvote.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsvote.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoleManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserRepository()
    val currentUid: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val users: StateFlow<List<com.example.smsvote.data.model.AppUser>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserRole: String
        get() = users.value.find { it.uid == currentUid }?.role ?: "operator"

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
