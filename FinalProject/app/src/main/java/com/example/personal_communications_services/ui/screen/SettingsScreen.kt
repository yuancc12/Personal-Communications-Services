package com.example.personal_communications_services.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personal_communications_services.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val context      = LocalContext.current
    val autoMonitor  by vm.autoMonitor.collectAsStateWithLifecycle()
    val llmEnabled   by vm.llmEnabled.collectAsStateWithLifecycle()
    val saveFullBody by vm.saveFullBody.collectAsStateWithLifecycle()
    val notifyParent by vm.notifyParent.collectAsStateWithLifecycle()
    val parentNumber by vm.parentNumber.collectAsStateWithLifecycle()
    val simStatus    by vm.simStatus.collectAsStateWithLifecycle()
    val scanStatus   by vm.scanStatus.collectAsStateWithLifecycle()
    var parentInput  by remember(parentNumber) { mutableStateOf(parentNumber) }

    // 切換「自動監聽」時請求 RECEIVE_SMS 執行期權限
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // 授予才真正開啟，拒絕則維持關閉
        vm.setAutoMonitor(granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("設定", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        SettingRow(
            title    = "自動監聽簡訊",
            subtitle = "收到簡訊時自動偵測風險（需授予 RECEIVE_SMS 權限）",
            checked  = autoMonitor,
            onChange = { enabled ->
                if (!enabled) {
                    vm.setAutoMonitor(false)
                    return@SettingRow
                }
                val already = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED
                if (already) {
                    vm.setAutoMonitor(true)
                } else {
                    // 跳出系統權限請求對話框
                    smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                }
            }
        )
        HorizontalDivider()
        SettingRow(
            title    = "啟用 JEV AI 判斷",
            subtitle = "收到簡訊時呼叫 TypeSafe JEV 進行風險分類與詐騙類型識別\n（需在 local.properties 設定 TYPESAFE_API_KEY）",
            checked  = llmEnabled,
            onChange = vm::setLlmEnabled
        )
        HorizontalDivider()
        SettingRow(
            title    = "保存完整簡訊內容",
            subtitle = "關閉時僅保存前 30 字摘要（預設關閉，保護隱私）",
            checked  = saveFullBody,
            onChange = vm::setSaveFullBody
        )
        HorizontalDivider()
        SettingRow(
            title    = "高風險時通知家長",
            subtitle = "偵測到高風險簡訊時，自動發送 SMS 警報給家長",
            checked  = notifyParent,
            onChange = vm::setNotifyParent
        )
        if (notifyParent) {
            OutlinedTextField(
                value         = parentInput,
                onValueChange = { parentInput = it },
                label         = { Text("家長手機號碼") },
                placeholder   = { Text("例：0912345678") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                trailingIcon  = {
                    TextButton(onClick = { vm.setParentNumber(parentInput) }) {
                        Text("儲存")
                    }
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("測試工具", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "模擬收到一封詐騙簡訊，觸發完整分析流程（規則引擎 → AI → 通知推播 → 存入紀錄）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { vm.simulateSms(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("模擬收到詐騙簡訊")
        }
        if (simStatus.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(simStatus, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(8.dp))

        val readSmsLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> if (granted) vm.scanInbox(context) }

        Button(
            onClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) vm.scanInbox(context)
                else readSmsLauncher.launch(Manifest.permission.READ_SMS)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("掃描歷史收件匣（最近 50 則）")
        }
        if (scanStatus.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(scanStatus, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color  = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
