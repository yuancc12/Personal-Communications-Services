package com.example.personal_communications_services.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personal_communications_services.data.BlacklistEntity
import com.example.personal_communications_services.data.DetectionRecordEntity
import com.example.personal_communications_services.model.RiskLevel
import com.example.personal_communications_services.model.ScamType
import com.example.personal_communications_services.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: HistoryViewModel = viewModel()) {
    val records        by vm.records.collectAsStateWithLifecycle()
    val blacklist      by vm.blacklist.collectAsStateWithLifecycle()
    val filterRisk     by vm.filterRisk.collectAsStateWithLifecycle()
    val filterScamType by vm.filterScamType.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) {
                Text("偵測紀錄", modifier = Modifier.padding(vertical = 12.dp))
            }
            Tab(selected = tab == 1, onClick = { tab = 1 }) {
                Text("黑名單", modifier = Modifier.padding(vertical = 12.dp))
            }
        }
        when (tab) {
            0 -> RecordListTab(
                records        = records,
                filterRisk     = filterRisk,
                filterScamType = filterScamType,
                onFilterRisk     = { vm.filterRisk.value = if (vm.filterRisk.value == it) null else it },
                onFilterScamType = { vm.filterScamType.value = if (vm.filterScamType.value == it) null else it },
                onDelete       = vm::deleteRecord,
                onClearAll     = vm::deleteAll
            )
            1 -> BlacklistTab(blacklist, onAdd = vm::addToBlacklist, onRemove = vm::removeFromBlacklist)
        }
    }
}

// ── 偵測紀錄頁 ────────────────────────────────────────────────────────────

@Composable
private fun RecordListTab(
    records: List<DetectionRecordEntity>,
    filterRisk: RiskLevel?,
    filterScamType: ScamType?,
    onFilterRisk: (RiskLevel) -> Unit,
    onFilterScamType: (ScamType) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    Column {
        // 篩選列
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RiskLevel.entries.forEach { level ->
                FilterChip(
                    selected = filterRisk == level,
                    onClick  = { onFilterRisk(level) },
                    label    = { Text(level.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
            ScamType.entries.filter { it != ScamType.NONE }.forEach { type ->
                FilterChip(
                    selected = filterScamType == type,
                    onClick  = { onFilterScamType(type) },
                    label    = { Text("${type.emoji} ${type.label}", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("沒有符合條件的紀錄")
            }
            return@Column
        }

        TextButton(
            onClick = onClearAll,
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 16.dp)
        ) { Text("清除全部", color = MaterialTheme.colorScheme.error) }

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(records, key = { it.id }) { record ->
                RecordCard(record = record, onDelete = { onDelete(record.id) })
            }
        }
    }
}

@Composable
private fun RecordCard(record: DetectionRecordEntity, onDelete: () -> Unit) {
    val level = runCatching { RiskLevel.valueOf(record.riskLevel) }.getOrNull()
    val bg = when (level) {
        RiskLevel.SAFE       -> Color(0xFFE8F5E9)
        RiskLevel.SUSPICIOUS -> Color(0xFFFFF8E1)
        RiskLevel.HIGH_RISK  -> Color(0xFFFFEBEE)
        null                 -> MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(level?.label ?: record.riskLevel, style = MaterialTheme.typography.titleSmall)
                    val scamType = runCatching { ScamType.valueOf(record.scamType) }.getOrNull()
                    if (scamType != null && scamType != ScamType.NONE) {
                        Text(
                            "${scamType.emoji} ${scamType.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(record.summary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                // 顯示信心度（reasons 第一條通常是 "🤖 JEV 信心度：XX%"）
                val confidence = record.reasons.split("|").firstOrNull { it.contains("信心度") }
                if (confidence != null) {
                    Text(
                        confidence,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    SimpleDateFormat("MM/dd HH:mm", Locale.TAIWAN).format(Date(record.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "刪除")
            }
        }
    }
}

// ── 黑名單頁 ──────────────────────────────────────────────────────────────

@Composable
private fun BlacklistTab(
    blacklist: List<BlacklistEntity>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var newNumber by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        // 新增號碼列
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newNumber,
                onValueChange = { newNumber = it },
                label = { Text("號碼") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newNumber.isNotBlank()) {
                        onAdd(newNumber)
                        newNumber = ""
                    }
                }
            ) { Text("加入") }
        }

        HorizontalDivider()

        if (blacklist.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("黑名單為空")
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                items(blacklist, key = { it.phoneNumber }) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.phoneNumber, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRemove(entry.phoneNumber) }) {
                                Icon(Icons.Default.Delete, contentDescription = "移除")
                            }
                        }
                    }
                }
            }
        }
    }
}
