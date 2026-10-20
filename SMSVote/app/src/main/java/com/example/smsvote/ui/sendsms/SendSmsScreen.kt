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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsvote.data.model.Poll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendSmsScreen(
    poll: Poll,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SendSmsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
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

        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = poll.question, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = poll.options.mapIndexed { i, opt -> "${'A' + i}: $opt" }.joinToString("  "),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.phoneNumbers,
                    onValueChange = viewModel::onPhoneNumbersChange,
                    label = { Text("收件者電話（多個請用逗號分隔）") },
                    placeholder = { Text("例：0912345678, 0987654321") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                    enabled = !state.isSending
                ) {
                    Text(if (state.isSending) "發送中…" else "發送簡訊")
                }
                if (state.deliveryStates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("發送狀態", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(state.deliveryStates) { delivery ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(delivery.phone, style = MaterialTheme.typography.bodyMedium)
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
        }
    }
}
