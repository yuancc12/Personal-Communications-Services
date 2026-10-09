package com.example.personal_communications_services.model

/** 偵測結果：包含分數、風險等級、命中原因，以及判斷來源 */
data class DetectionResult(
    val score: Int,              // 0-100 加權分數
    val riskLevel: RiskLevel,
    val reasons: List<String>,   // 命中原因清單（供 UI 顯示）
    val source: ResultSource = ResultSource.RULE
)

enum class ResultSource { RULE, LLM }
