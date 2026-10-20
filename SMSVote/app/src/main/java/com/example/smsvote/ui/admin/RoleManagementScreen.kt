package com.example.smsvote.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsvote.data.model.AppUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleManagementScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoleManagementViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("帳號權限管理") },
            navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } }
        )

        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("共 ${users.size} 個帳號", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(users, key = { it.uid }) { user ->
                UserRoleCard(
                    user = user,
                    isSelf = user.uid == viewModel.currentUid,
                    onSetAdmin = { viewModel.setAdmin(user.uid) },
                    onSetOperator = { viewModel.setOperator(user.uid) }
                )
            }
        }
    }
}

@Composable
private fun UserRoleCard(
    user: AppUser,
    isSelf: Boolean,
    onSetAdmin: () -> Unit,
    onSetOperator: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${user.displayName}${if (isSelf) "（你）" else ""}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (user.isAdmin) "管理者" else "操作員",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (user.isAdmin) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary
                )
            }
            if (!isSelf) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!user.isAdmin) {
                        Button(onClick = onSetAdmin, modifier = Modifier.weight(1f)) {
                            Text("升為管理者")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSetOperator,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("降為操作員")
                        }
                    }
                }
            }
        }
    }
}
