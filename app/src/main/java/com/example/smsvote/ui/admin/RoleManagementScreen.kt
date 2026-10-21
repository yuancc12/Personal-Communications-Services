package com.example.smsvote.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsvote.data.model.AppUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleManagementScreen(
    currentUserRole: String,
    onBack: () -> Unit,
    onGenerateInvite: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoleManagementViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val pending = users.filter { it.isPending }
    val approved = users.filter { !it.isPending }
    val isSuperAdmin = currentUserRole == "super_admin"

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("帳號權限管理", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── Invite button ──
            item {
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onGenerateInvite,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("建立邀請碼") }
                Spacer(Modifier.height(4.dp))
            }

            // ── Pending users ──
            if (pending.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "待審核 ${pending.size}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                items(pending, key = { "pending_${it.uid}" }) { user ->
                    PendingUserCard(
                        user = user,
                        onApprove = { viewModel.approveUser(user.uid) },
                        onReject = { viewModel.rejectUser(user.uid) }
                    )
                }
            }

            // ── Approved users ──
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "已核准帳號（${approved.size} 人）",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isSuperAdmin) {
                    Text(
                        "只有總管理員可以更改已核准帳號的角色",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            items(approved, key = { it.uid }) { user ->
                ApprovedUserCard(
                    user = user,
                    isSelf = user.uid == viewModel.currentUid,
                    isSuperAdmin = isSuperAdmin,
                    onSetAdmin = { viewModel.setAdmin(user.uid) },
                    onSetOperator = { viewModel.setOperator(user.uid) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PendingUserCard(user: AppUser, onApprove: () -> Unit, onReject: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(user.email, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "待審核",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("核准") }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("拒絕") }
            }
        }
    }
}

@Composable
private fun ApprovedUserCard(
    user: AppUser,
    isSelf: Boolean,
    isSuperAdmin: Boolean,
    onSetAdmin: () -> Unit,
    onSetOperator: () -> Unit
) {
    val roleBadgeColor = when (user.role) {
        "super_admin" -> MaterialTheme.colorScheme.tertiaryContainer
        "admin" -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val roleBadgeTextColor = when (user.role) {
        "super_admin" -> MaterialTheme.colorScheme.onTertiaryContainer
        "admin" -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val roleText = when (user.role) {
        "super_admin" -> "👑 總管理員"
        "admin" -> "⭐ 管理員"
        else -> "操作員"
    }

    // super_admin can change any non-super_admin; admin cannot change roles at all
    val canChangeRole = isSuperAdmin && !isSelf && user.role != "super_admin"

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName + if (isSelf) "（你）" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(user.email, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = roleBadgeColor,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = roleText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = roleBadgeTextColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (canChangeRole) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (user.role != "admin") {
                        Button(
                            onClick = onSetAdmin,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("升為管理員") }
                    }
                    if (user.role != "operator") {
                        OutlinedButton(
                            onClick = onSetOperator,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("降為操作員") }
                    }
                }
            }
        }
    }
}
