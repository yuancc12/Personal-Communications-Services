package com.example.smsvote

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsvote.auth.AuthManager
import com.example.smsvote.data.model.Poll
import com.example.smsvote.ui.AdminHomeScreen
import com.example.smsvote.ui.LoginScreen
import com.example.smsvote.ui.admin.AdminViewModel
import com.example.smsvote.ui.admin.RoleManagementScreen
import com.example.smsvote.ui.createpoll.CreatePollScreen
import com.example.smsvote.ui.invite.InviteQrScreen
import com.example.smsvote.ui.invite.ScanQrScreen
import com.example.smsvote.ui.results.PollResultsScreen
import com.example.smsvote.ui.sendsms.SendSmsScreen
import com.example.smsvote.ui.theme.SMSVoteTheme
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object CreatePoll : Screen()
    object RoleManagement : Screen()
    object ScanInvite : Screen()
    data class GenerateInvite(val isSuperAdmin: Boolean) : Screen()
    data class SendSms(val poll: Poll) : Screen()
    data class PollResults(val poll: Poll) : Screen()
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
        } else if (currentAppUser?.isPending == true) {
            PendingApprovalScreen(
                displayName = user!!.displayName ?: "",
                onSignOut = { authManager.signOut(); user = null },
                onScanInvite = { currentScreen = Screen.ScanInvite },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            when (val screen = currentScreen) {
                Screen.Home -> AdminHomeScreen(
                    displayName = user!!.displayName ?: "管理者",
                    userEmail = user!!.email ?: "",
                    polls = polls,
                    isAdmin = currentAppUser?.isAdmin == true,
                    isSuperAdmin = currentAppUser?.isSuperAdmin == true,
                    roleLabel = currentAppUser?.roleLabel ?: "操作員",
                    onSignOut = { authManager.signOut(); user = null },
                    onCreatePoll = { currentScreen = Screen.CreatePoll },
                    onManageRoles = { currentScreen = Screen.RoleManagement },
                    onGenerateInvite = { currentScreen = Screen.GenerateInvite(currentAppUser?.isSuperAdmin == true) },
                    onSendSms = { poll -> currentScreen = Screen.SendSms(poll) },
                    onDeletePoll = { pollId -> adminViewModel.deletePoll(pollId) },
                    onViewResults = { poll -> currentScreen = Screen.PollResults(poll) },
                    modifier = Modifier.padding(innerPadding)
                )
                Screen.CreatePoll -> CreatePollScreen(
                    onBack = { currentScreen = Screen.Home },
                    modifier = Modifier.padding(innerPadding)
                )
                Screen.RoleManagement -> RoleManagementScreen(
                    currentUserRole = currentAppUser?.role ?: "operator",
                    onBack = { currentScreen = Screen.Home },
                    onGenerateInvite = { currentScreen = Screen.GenerateInvite(currentAppUser?.isSuperAdmin == true) },
                    modifier = Modifier.padding(innerPadding)
                )
                Screen.ScanInvite -> ScanQrScreen(
                    onBack = { currentScreen = Screen.Home },
                    onSuccess = {
                        adminViewModel.loadUser()
                        currentScreen = Screen.Home
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                is Screen.GenerateInvite -> InviteQrScreen(
                    isSuperAdmin = screen.isSuperAdmin,
                    onBack = { currentScreen = Screen.Home },
                    modifier = Modifier.padding(innerPadding)
                )
                is Screen.SendSms -> SendSmsScreen(
                    poll = screen.poll,
                    onBack = { currentScreen = Screen.Home },
                    modifier = Modifier.padding(innerPadding)
                )
                is Screen.PollResults -> PollResultsScreen(
                    poll = screen.poll,
                    onBack = { currentScreen = Screen.Home },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun PendingApprovalScreen(
    displayName: String,
    onSignOut: () -> Unit,
    onScanInvite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⏳", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "等待管理者審核",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "您好，$displayName\n\n您的帳號已建立，請聯繫管理者核准您的存取權限。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        FilledTonalButton(
            onClick = onScanInvite,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) { Text("📷 我有邀請碼") }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) { Text("登出", fontWeight = FontWeight.SemiBold) }
    }
}
