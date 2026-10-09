package com.example.personal_communications_services

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.personal_communications_services.notification.NotificationHelper
import com.example.personal_communications_services.ui.screen.DetectScreen
import com.example.personal_communications_services.ui.screen.HistoryScreen
import com.example.personal_communications_services.ui.screen.SettingsScreen
import com.example.personal_communications_services.ui.theme.PersonalCommunicationsServicesTheme

class MainActivity : ComponentActivity() {

    // 執行期權限請求（Android 13+ 需要 POST_NOTIFICATIONS）
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 使用者決定後不強制，功能仍可正常使用（只是沒有通知） */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        requestRuntimePermissions()
        enableEdgeToEdge()
        setContent {
            PersonalCommunicationsServicesTheme {
                ScamDetectorApp()
            }
        }
    }

    private fun requestRuntimePermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            // RECEIVE_SMS 由使用者在設定頁切換自動監聽時再請求
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }
}

// ── 導航路由定義 ──────────────────────────────────────────────────────────

private sealed class Screen(val route: String, val label: String) {
    object Detect   : Screen("detect",   "檢測")
    object History  : Screen("history",  "紀錄")
    object Settings : Screen("settings", "設定")
}

// ── App 骨架（底部 NavigationBar + NavHost）────────────────────────────

@Composable
private fun ScamDetectorApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val tabs = listOf(Screen.Detect, Screen.History, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { screen ->
                    NavigationBarItem(
                        selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                        onClick  = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon = {
                            when (screen) {
                                Screen.Detect   -> Icon(Icons.Default.Search,   contentDescription = null)
                                Screen.History  -> Icon(Icons.Default.History,  contentDescription = null)
                                Screen.Settings -> Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Detect.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Detect.route)   { DetectScreen() }
            composable(Screen.History.route)  { HistoryScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
