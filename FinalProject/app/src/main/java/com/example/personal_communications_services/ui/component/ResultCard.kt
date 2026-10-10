package com.example.personal_communications_services.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personal_communications_services.model.DetectionResult
import com.example.personal_communications_services.model.ResultSource
import com.example.personal_communications_services.model.RiskLevel
import com.example.personal_communications_services.model.ScamType

@Composable
fun ResultCard(result: DetectionResult, modifier: Modifier = Modifier) {
    val bg = when (result.riskLevel) {
        RiskLevel.SAFE      -> Color(0xFFE8F5E9)
        RiskLevel.SUSPICIOUS -> Color(0xFFFFF8E1)
        RiskLevel.HIGH_RISK  -> Color(0xFFFFEBEE)
    }
    val accent = when (result.riskLevel) {
        RiskLevel.SAFE      -> Color(0xFF2E7D32)
        RiskLevel.SUSPICIOUS -> Color(0xFFF57F17)
        RiskLevel.HIGH_RISK  -> Color(0xFFC62828)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = result.riskLevel.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                if (result.confidence != null) {
                    Text(
                        text = "信心度 ${"%.0f".format(result.confidence * 100)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            }

            HorizontalDivider(color = accent.copy(alpha = 0.3f))

            if (result.reasons.isEmpty()) {
                Text("✅ 未偵測到可疑特徵", style = MaterialTheme.typography.bodyMedium)
            } else {
                result.reasons.forEach { reason ->
                    Text(reason, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (result.scamType != null && result.scamType != ScamType.NONE) {
                Text(
                    text = "${result.scamType.emoji} 類型：${result.scamType.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = accent
                )
            }

            if (result.source == ResultSource.LLM) {
                Text(
                    "由 JEV 判斷",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.7f)
                )
            }
        }
    }
}
