package com.example.personal_communications_services.model

/** 風險等級：依分數範圍分為三級 */
enum class RiskLevel(val label: String) {
    SAFE("安全"),
    SUSPICIOUS("可疑"),
    HIGH_RISK("高風險")
}

fun Int.toRiskLevel(): RiskLevel = when {
    this <= 30 -> RiskLevel.SAFE
    this <= 70 -> RiskLevel.SUSPICIOUS
    else       -> RiskLevel.HIGH_RISK
}
