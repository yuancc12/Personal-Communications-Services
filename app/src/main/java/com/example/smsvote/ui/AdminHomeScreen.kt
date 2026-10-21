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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var activeTab by remember { mutableStateOf(HomeTab.POLLS) }
    var deleteTarget by remember { mutableStateOf<Poll?>(null) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (activeTab) {
                            HomeTab.POLLS -> "SMS 匿名投票"
                            HomeTab.RESULTS -> "投票結果"
                            HomeTab.CONTACTS -> "管理聯絡人"
                            HomeTab.ACCOUNT -> "我的帳號"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
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

@Composable
private fun AccountTabContent(
    displayName: String,
    userEmail: String,
    isAdmin: Boolean,
    isSuperAdmin: Boolean,
    roleLabel: String,
    onManageRoles: () -> Unit,
    onGenerateInvite: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
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
            Spacer(Modifier.height(12.dp))
        }
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
