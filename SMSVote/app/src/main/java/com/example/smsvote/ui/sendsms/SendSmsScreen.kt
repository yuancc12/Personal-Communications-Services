package com.example.smsvote.ui.sendsms

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsvote.data.model.Poll
import com.example.smsvote.data.model.Recipient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendSmsScreen(
    poll: Poll,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SendSmsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val recipients by viewModel.recipients.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.sendSms(poll)
    }

    LaunchedEffect(Unit) { viewModel.resetForNewSession() }
    LaunchedEffect(state.allSent) { if (state.allSent) onBack() }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("發送投票簡訊") },
            navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } }
        )

        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
            // ── Poll summary card ──
            item {
                Spacer(Modifier.height(8.dp))
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(poll.question, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        poll.options.forEachIndexed { i, opt ->
                            Text("${'A' + i}：$opt", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Recipients section header ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "選擇聯絡人（已選 ${selectedIds.size} 人）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row {
                        TextButton(onClick = viewModel::selectAll, enabled = recipients.isNotEmpty()) {
                            Text("全選", style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = viewModel::clearAll, enabled = selectedIds.isNotEmpty()) {
                            Text("清除", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (recipients.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "尚無聯絡人，請至「聯絡人」頁面新增",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(recipients, key = { it.id }) { recipient ->
                    RecipientCheckItem(
                        recipient = recipient,
                        checked = recipient.id in selectedIds,
                        onToggle = { viewModel.toggleRecipient(recipient.id) }
                    )
                }
            }

            // ── Extra numbers text field ──
            item {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.extraNumbers,
                    onValueChange = viewModel::onExtraNumbersChange,
                    label = { Text("其他號碼（選填）") },
                    placeholder = { Text("例：0912345678, 0987654321") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.SEND_SMS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) viewModel.sendSms(poll)
                        else permissionLauncher.launch(Manifest.permission.SEND_SMS)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isSending
                ) {
                    Text(if (state.isSending) "發送中…" else "發送簡訊")
                }
                if (state.deliveryStates.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("發送狀態", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Delivery status list ──
            items(state.deliveryStates) { delivery ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (delivery.name.isNotEmpty()) {
                            Text(delivery.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Text(delivery.phone, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = when (delivery.status) {
                            SmsStatus.PENDING -> "⏳ 等待中"
                            SmsStatus.SENT -> "✓ 已送出"
                            SmsStatus.DELIVERED -> "✓✓ 已送達"
                            SmsStatus.FAILED -> "✗ 失敗"
                        },
                        color = when (delivery.status) {
                            SmsStatus.DELIVERED -> Color(0xFF2E7D32)
                            SmsStatus.SENT -> MaterialTheme.colorScheme.primary
                            SmsStatus.FAILED -> MaterialTheme.colorScheme.error
                            SmsStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RecipientCheckItem(
    recipient: Recipient,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(recipient.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(recipient.phoneNumber, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
