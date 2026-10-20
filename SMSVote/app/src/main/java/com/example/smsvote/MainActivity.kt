package com.example.smsvote

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.smsvote.auth.AuthManager
import com.example.smsvote.data.model.Poll
import com.example.smsvote.ui.AdminHomeScreen
import com.example.smsvote.ui.LoginScreen
import com.example.smsvote.ui.admin.AdminViewModel
import com.example.smsvote.ui.admin.RoleManagementScreen
import com.example.smsvote.ui.createpoll.CreatePollScreen
import com.example.smsvote.ui.sendsms.SendSmsScreen
import com.example.smsvote.ui.theme.SMSVoteTheme
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object CreatePoll : Screen()
    object RoleManagement : Screen()
    data class SendSms(val poll: Poll) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)
        enableEdgeToEdge()
        setContent {
            SMSVoteTheme {
                AppRoot(authManager)
            }
        }
    }
}

@Composable
private fun AppRoot(authManager: AuthManager) {
    var user by remember { mutableStateOf(authManager.currentUser) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val scope = rememberCoroutineScope()
    val adminViewModel: AdminViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val polls by adminViewModel.polls.collectAsState()
    val currentAppUser by adminViewModel.currentUser.collectAsState()

    LaunchedEffect(user) {
        if (user != null) adminViewModel.loadUser()
    }

    val receiveSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 靜默處理 */ }

    LaunchedEffect(user) {
        if (user != null) receiveSmsLauncher.launch(Manifest.permission.RECEIVE_SMS)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (user == null) {
            LoginScreen(
                onSignInClick = {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        authManager.signInWithGoogle().fold(
                            onSuccess = { u ->
                                user = u
                                isLoading = false
                                adminViewModel.loadUser()
                            },
                            onFailure = { e ->
                                errorMessage = "登入失敗：${e.localizedMessage}"
                                Log.e("Auth", "Sign-in failed", e)
                                isLoading = false
                            }
                        )
                    }
                },
                isLoading = isLoading,
                errorMessage = errorMessage,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (val screen = currentScreen) {
                Screen.Home -> AdminHomeScreen(
                    displayName = user!!.displayName ?: "管理者",
                    userEmail = user!!.email ?: "",
                    polls = polls,
                    isAdmin = currentAppUser?.isAdmin == true,
                    onSignOut = { authManager.signOut(); user = null },
                    onCreatePoll = { currentScreen = Screen.CreatePoll },
                    onManageRoles = { currentScreen = Screen.RoleManagement },
                    onSendSms = { poll -> currentScreen = Screen.SendSms(poll) },
                    onDeletePoll = { pollId -> adminViewModel.deletePoll(pollId) },
                    modifier = Modifier.padding(innerPadding)
                )
                Screen.CreatePoll -> CreatePollScreen(
                    onBack = { currentScreen = Screen.Home },
                    modifier = Modifier.padding(innerPadding)
                )
                Screen.RoleManagement -> RoleManagementScreen(
                    onBack = { currentScreen = Screen.Home },
                    modifier = Modifier.padding(innerPadding)
                )
                is Screen.SendSms -> SendSmsScreen(
                    poll = screen.poll,
                    onBack = { currentScreen = Screen.Home },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
