package com.example.personal_communications_services.model

/** 偵測結果：包含分數、風險等級、命中原因，以及判斷來源 */
data class DetectionResult(
    val score: Int,                   // 代表性分數（safe=10, suspicious=50, high=85）
    val riskLevel: RiskLevel,
    val reasons: List<String>,        // 命中原因清單（供 UI 顯示）
    val source: ResultSource = ResultSource.RULE,
    val confidence: Double? = null,   // JEV 回傳的信心度 0.0~1.0
    val scamType: ScamType? = null    // JEV 回傳的詐騙類型
)

enum class ResultSource { RULE, LLM }
