@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.smsvote.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsvote.data.model.AppUser
import com.example.smsvote.data.model.Poll
import com.example.smsvote.data.repository.PollRepository
import com.example.smsvote.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val pollRepository = PollRepository()
    private val userRepository = UserRepository()

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    val polls: StateFlow<List<Poll>> = _currentUser
        .flatMapLatest { user ->
            when {
                user == null -> kotlinx.coroutines.flow.flowOf(emptyList())
                user.isAdmin -> pollRepository.getAllPolls()
                else -> pollRepository.getPollsByCreator(user.uid)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deletePoll(pollId: String) {
        viewModelScope.launch {
            runCatching { pollRepository.deletePoll(pollId) }
        }
    }

    fun loadUser() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            runCatching {
                userRepository.getOrCreateUser(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: ""
                )
            }.onSuccess { _currentUser.value = it }
              .onFailure { android.util.Log.e("AdminViewModel", "loadUser failed", it) }
        }
    }
}
