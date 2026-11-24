package com.example.smsvote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.smsvote.data.model.Poll
import com.example.smsvote.ui.recipients.ManageRecipientsScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HomeTab(val label: String, val icon: ImageVector) {
    POLLS("投票", Icons.Default.Home),
    RESULTS("結果", Icons.Default.Star),
    CONTACTS("聯絡人", Icons.Default.Person),
    ACCOUNT("帳號", Icons.Default.Settings)
}

private val THEMES: List<List<Color>> = listOf(
    listOf(Color(0xFF1565C0), Color(0xFF0288D1), Color(0xFFF9A825)),
    listOf(Color(0xFF00695C), Color(0xFF00897B), Color(0xFFFFB300)),
    listOf(Color(0xFFB71C1C), Color(0xFFE53935), Color(0xFFFF6F00)),
    listOf(Color(0xFF283593), Color(0xFF1976D2), Color(0xFF26C6DA)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    displayName: String,
    userEmail: String,
    polls: List<Poll>,
    isAdmin: Boolean,
    isSuperAdmin: Boolean,
    roleLabel: String,
    onSignOut: () -> Unit,
    onCreatePoll: () -> Unit,
    onManageRoles: () -> Unit,
    onGenerateInvite: () -> Unit,
    onSendSms: (Poll) -> Unit,
    onDeletePoll: (String) -> Unit,
    onViewResults: (Poll) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(HomeTab.POLLS) }
    var deleteTarget by remember { mutableStateOf<Poll?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var themeIndex by rememberSaveable { mutableStateOf(getThemeIndex(context)) }

    // Auto-show help on first login (unless dismissed)
    LaunchedEffect(Unit) {
        if (!hasSeenOnboarding(context)) showHelpDialog = true
    }
    if (showHelpDialog) HelpDialog(onDismiss = { showHelpDialog = false })

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(colors = THEMES[themeIndex])
                    )
            ) {
                TopAppBar(
                    title = {
                        Text(
                            when (activeTab) {
                                HomeTab.POLLS -> "SMS 匿名投票"
                                HomeTab.RESULTS -> "投票結果"
                                HomeTab.CONTACTS -> "管理聯絡人"
                                HomeTab.ACCOUNT -> "我的帳號"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    actions = {
                        IconButton(onClick = { showHelpDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = "說明", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab == HomeTab.POLLS) {
                ExtendedFloatingActionButton(
                    onClick = onCreatePoll,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("建立投票") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        when (activeTab) {
            HomeTab.POLLS -> PollsTabContent(
                polls = polls,
                onSendSms = onSendSms,
                onDelete = { deleteTarget = it },
                onViewResults = onViewResults,
                modifier = Modifier.padding(innerPadding)
            )
            HomeTab.RESULTS -> ResultsTabContent(
                polls = polls,
                onViewResults = onViewResults,
                modifier = Modifier.padding(innerPadding)
            )
            HomeTab.CONTACTS -> ManageRecipientsScreen(
                modifier = Modifier.padding(innerPadding)
            )
            HomeTab.ACCOUNT -> AccountTabContent(
                displayName = displayName,
                userEmail = userEmail,
                isAdmin = isAdmin,
                isSuperAdmin = isSuperAdmin,
                roleLabel = roleLabel,
                themeIndex = themeIndex,
                onThemeChange = { i -> themeIndex = i; saveThemeIndex(context, i) },
                onManageRoles = onManageRoles,
                onGenerateInvite = onGenerateInvite,
                onSignOut = onSignOut,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    deleteTarget?.let { poll ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("確認刪除") },
            text = { Text("刪除「${poll.question}」？此操作無法復原。") },
            confirmButton = {
                TextButton(onClick = { onDeletePoll(poll.id); deleteTarget = null }) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun PollsTabContent(
    polls: List<Poll>,
    onSendSms: (Poll) -> Unit,
    onDelete: (Poll) -> Unit,
    onViewResults: (Poll) -> Unit,
    modifier: Modifier = Modifier
) {
    if (polls.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🗳", fontSize = 52.sp)
                Spacer(Modifier.height(16.dp))
                Text("還沒有投票", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("點擊右下角「建立投票」開始", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(polls, key = { it.id }) { poll ->
                PollCard(
                    poll = poll,
                    onSendSms = { onSendSms(poll) },
                    onDelete = { onDelete(poll) },
                    onViewResults = { onViewResults(poll) }
                )
            }
        }
    }
}

@Composable
private fun ResultsTabContent(
    polls: List<Poll>,
    onViewResults: (Poll) -> Unit,
    modifier: Modifier = Modifier
) {
    if (polls.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📊", fontSize = 52.sp)
                Spacer(Modifier.height(16.dp))
                Text("還沒有投票資料", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("建立投票後結果將顯示在此", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(polls, key = { it.id }) { poll ->
                ResultSummaryCard(poll = poll, onViewResults = { onViewResults(poll) })
            }
        }
    }
}

@Composable
private fun ResultSummaryCard(poll: Poll, onViewResults: () -> Unit) {
    val isExpired = poll.endTime != null && System.currentTimeMillis() > poll.endTime
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = poll.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Surface(
                    color = if (isExpired) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isExpired) "已截止" else "進行中",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isExpired) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            if (poll.endTime != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "截止：${dateFormat.format(Date(poll.endTime))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onViewResults,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("📊 查看即時結果")
            }
        }
    }
}

@Composable
private fun PollCard(poll: Poll, onSendSms: () -> Unit, onDelete: () -> Unit, onViewResults: () -> Unit) {
    val isExpired = poll.endTime != null && System.currentTimeMillis() > poll.endTime
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = poll.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                if (poll.category.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = poll.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Surface(
                    color = if (isExpired) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isExpired) "已截止" else "進行中",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isExpired) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            val meta = buildString {
                if (poll.maxChoices > 1) append("最多選 ${poll.maxChoices} 個  ")
                if (poll.endTime != null) append("截止：${dateFormat.format(Date(poll.endTime))}")
            }.trim()
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            poll.options.forEachIndexed { i, opt ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ('A' + i).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(opt, style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onViewResults,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("查看結果") }
                FilledTonalButton(
                    onClick = onSendSms,
                    modifier = Modifier.weight(1f),
                    enabled = !isExpired,
                    shape = RoundedCornerShape(10.dp)
                ) { Text("發送通知") }
                TextButton(onClick = onDelete) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSettingsDialog(onDismiss: () -> Unit) {
    val context  = LocalContext.current
    val providers = listOf("gemini" to "Gemini（Google）", "openai" to "GPT（OpenAI）", "claude" to "Claude（Anthropic）")
    var selectedProvider by rememberSaveable { mutableStateOf(getAiProvider(context)) }
    var apiKey  by rememberSaveable { mutableStateOf(getAiApiKey(context, selectedProvider)) }
    var expanded by remember { mutableStateOf(false) }
    var saved   by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("選擇 AI 服務並輸入對應的 API Key，Key 儲存在裝置本機。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = providers.first { it.first == selectedProvider }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("AI 服務") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        providers.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedProvider = value
                                    apiKey = getAiApiKey(context, value)
                                    expanded = false
                                    saved = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; saved = false },
                    label = { Text("API Key") },
                    placeholder = { Text("貼上 API Key…") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (saved) Text("✓ 已儲存", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium)
            }
        },
        confirmButton = {
            Button(onClick = {
                saveAiProvider(context, selectedProvider)
                saveAiApiKey(context, selectedProvider, apiKey.trim())
                saved = true
            }) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("關閉") } }
    )
}

@Composable
private fun AccountTabContent(
    displayName: String,
    userEmail: String,
    isAdmin: Boolean,
    isSuperAdmin: Boolean,
    roleLabel: String,
    themeIndex: Int = 0,
    onThemeChange: (Int) -> Unit = {},
    onManageRoles: () -> Unit,
    onGenerateInvite: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAiDialog by remember { mutableStateOf(false) }
    if (showAiDialog) AiSettingsDialog(onDismiss = { showAiDialog = false })

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 漸變頭部卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(colors = THEMES[themeIndex]))
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(16.dp))
                // 主題切換色點
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    THEMES.forEachIndexed { i, colors ->
                        val isActive = i == themeIndex
                        Box(
                            modifier = Modifier
                                .size(if (isActive) 30.dp else 22.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (isActive) 1f else 0.5f))
                                .padding(if (isActive) 2.5.dp else 1.5.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(colors = listOf(colors[0], colors[2])))
                                .clickable { onThemeChange(i) }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(Modifier.height(20.dp))
        Surface(
            color = when {
                isSuperAdmin -> MaterialTheme.colorScheme.tertiaryContainer
                isAdmin -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            },
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = when {
                    isSuperAdmin -> "👑 $roleLabel"
                    isAdmin -> "⭐ $roleLabel"
                    else -> roleLabel
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = when {
                    isSuperAdmin -> MaterialTheme.colorScheme.onTertiaryContainer
                    isAdmin -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (isSuperAdmin) {
            Spacer(Modifier.height(8.dp))
            Text(
                "最高權限：可管理所有帳號與角色",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (isAdmin) {
            Spacer(Modifier.height(8.dp))
            Text(
                "可審核新帳號，可建立並管理投票",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                "可建立投票並查看自己的投票結果",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(48.dp))
        if (isAdmin) {
            OutlinedButton(onClick = onManageRoles, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("帳號權限管理")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onGenerateInvite, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("建立邀請碼")
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = { showAiDialog = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("🤖 AI 設定（API Key）")
        }
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
        } // end inner Column
    }
}

private data class HelpStep(
    val emoji: String,
    val title: String,
    val desc: String,
    val locationHint: String,
)

private val helpSteps = listOf(
    HelpStep("🗳️", "建立投票", "輸入題目與選項（A/B/C…），可設截止時間與複選上限。", "投票頁 → 右下角「＋ 建立投票」"),
    HelpStep("📋", "新增投票對象", "新增受訪者手機號碼（+886 格式）建立聯絡人名冊。", "底部導覽 → 聯絡人 → 新增"),
    HelpStep("📱", "發送 SMS 通知", "選擇聯絡人後確認，系統透過裝置 SIM 卡發送含題目的 SMS。", "投票卡片 → 「發送通知」"),
    HelpStep("💬", "受訪者回覆", "受訪者回傳「A」或「B,C」（複選），App 自動計票。重複回覆以最後一次為準。", "受訪者自行回覆 SMS 即可"),
    HelpStep("📊", "即時查看結果", "即時顯示票數，支援長條圖 / 圓餅圖，可匯出 PDF。", "底部導覽 → 結果"),
    HelpStep("🤖", "AI 智能分析", "AI 自動生成摘要與建議，需先在帳號頁設定 API Key。", "結果頁 → 「🤖 AI 分析」"),
    HelpStep("⚙️", "AI 設定", "選擇 Gemini / GPT / Claude，貼上 API Key，儲存在裝置本機。", "帳號頁 → 「🤖 AI 設定」"),
    HelpStep("👥", "帳號管理", "審核待批帳號，建立邀請碼讓他人掃描 QR Code 加入系統。", "帳號頁 → 「帳號權限管理」"),
)

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState { helpSteps.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == helpSteps.size - 1

    AlertDialog(
        onDismissRequest = {
            markOnboardingSeen(context)
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📖 操作說明", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    "${pagerState.currentPage + 1} / ${helpSteps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                // 分頁圓點指示
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    repeat(helpSteps.size) { i ->
                        val active = pagerState.currentPage == i
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (active) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                )
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.height(230.dp)
                ) { page ->
                    val step = helpSteps[page]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(step.emoji, fontSize = 52.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        // 紅色位置提示 badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFE53935))
                            )
                            Text(
                                step.locationHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB71C1C),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            step.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            step.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (isLast) {
                    markOnboardingSeen(context)
                    onDismiss()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            }) {
                Text(if (isLast) "知道了 ✓" else "下一步 →")
            }
        },
        dismissButton = {
            TextButton(onClick = { markOnboardingSeen(context); onDismiss() }) {
                Text(if (isLast) "關閉" else "跳過")
            }
        }
    )
}
