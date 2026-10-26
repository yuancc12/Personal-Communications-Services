package com.example.smsvote.ui.results

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsvote.data.model.Poll
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ChartType(val label: String) {
    BAR("長條圖"),
    PIE("圓餅圖")
}

private val CHART_COLORS = listOf(
    Color(0xFF1565C0),
    Color(0xFF2E7D32),
    Color(0xFFE65100),
    Color(0xFF6A1B9A),
    Color(0xFFC62828)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollResultsScreen(
    poll: Poll,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PollResultsViewModel = viewModel(factory = PollResultsViewModel.Factory(poll))
) {
    val results by viewModel.results.collectAsState()
    val totalVotes by viewModel.totalVotes.collectAsState()
    val sentCount by viewModel.sentCount.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()
    val isExpired = poll.endTime != null && System.currentTimeMillis() > poll.endTime
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    val context = LocalContext.current
    var chartType by remember { mutableStateOf(ChartType.BAR) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("即時結果", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Poll summary card ──
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                poll.question,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
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
                        if (poll.endTime != null || poll.maxChoices > 1) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                buildString {
                                    if (poll.maxChoices > 1) append("複選 最多 ${poll.maxChoices} 個  ")
                                    if (poll.endTime != null) append("截止：${dateFormat.format(Date(poll.endTime))}")
                                }.trim(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Total votes ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$totalVotes",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "票",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            // ── Response rate ──
            item {
                if (sentCount > 0) {
                    val rate = totalVotes.toFloat() / sentCount.toFloat()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "發出 $sentCount 人　·　回覆 $totalVotes 票　·　回覆率 ${"%.0f".format(rate * 100)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // ── Chart type selector ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    ChartType.entries.forEach { type ->
                        FilterChip(
                            selected = chartType == type,
                            onClick = { chartType = type },
                            label = { Text(type.label) }
                        )
                    }
                }
            }

            item { HorizontalDivider() }

            // ── Chart ──
            if (chartType == ChartType.BAR) {
                items(results) { result ->
                    val maxCount = results.maxOfOrNull { it.count } ?: 0
                    ResultBar(result = result, isLeading = result.count == maxCount && result.count > 0)
                }
            } else {
                item {
                    PieChartView(
                        results = results,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            item { HorizontalDivider() }

            // ── AI Analysis ──
            item {
                AiAnalysisSection(
                    state = analysisState,
                    onGenerate = viewModel::generateAnalysis
                )
            }

            // ── Export PDF button ──
            item {
                val analysisText = (analysisState as? AnalysisState.Success)?.text
                Button(
                    onClick = {
                        PdfExporter.shareAsPdf(context, poll, results, totalVotes, analysisText)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("匯出 PDF 報告", fontWeight = FontWeight.SemiBold)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PieChartView(results: List<OptionResult>, modifier: Modifier = Modifier) {
    val total = results.sumOf { it.count }.toFloat()
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }
    val reveal by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "pie_reveal"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (total == 0f) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("尚無票數", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Canvas(modifier = Modifier.size(220.dp)) {
                val totalAngle = reveal * 360f
                var startAngle = -90f
                var drawn = 0f
                results.forEachIndexed { index, result ->
                    if (result.count == 0) return@forEachIndexed
                    val sweep = (result.count / total) * 360f
                    val gap = if (results.count { it.count > 0 } > 1) 1.5f else 0f
                    val actualSweep = minOf(sweep - gap, totalAngle - drawn)
                    if (actualSweep <= 0f) return@forEachIndexed
                    drawArc(
                        color = CHART_COLORS[index % CHART_COLORS.size],
                        startAngle = startAngle,
                        sweepAngle = actualSweep,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweep
                    drawn += sweep
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            results.forEachIndexed { index, result ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(CHART_COLORS[index % CHART_COLORS.size])
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${result.label}  ${result.option}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${result.count} 票（${"%.0f".format(result.percent * 100)}%）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AiAnalysisSection(state: AnalysisState, onGenerate: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = Color(0xFF7C3AED),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "✦ AI 分析",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("Gemini 自動生成", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))

        when (state) {
            is AnalysisState.Idle -> {
                FilledTonalButton(
                    onClick = onGenerate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("✦ 生成 AI 分析摘要")
                }
            }
            is AnalysisState.Loading -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("AI 分析中…", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is AnalysisState.Success -> {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(state.text, style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp)
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = onGenerate,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("重新生成", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            is AnalysisState.Error -> {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("分析失敗：${state.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = onGenerate) { Text("重試") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultBar(result: OptionResult, isLeading: Boolean) {
    val animatedPercent by animateFloatAsState(
        targetValue = result.percent,
        animationSpec = tween(durationMillis = 700),
        label = "bar_${result.label}"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    color = if (isLeading) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        result.label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isLeading) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(result.option, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
            }
            Text(
                "${result.count} 票（${"%.0f".format(result.percent * 100)}%）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedPercent)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isLeading) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}
