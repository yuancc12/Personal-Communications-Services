package com.example.personal_communications_services.data

/** 偵測紀錄資料模型（使用 Gson JSON 檔案儲存，不需 Room/KSP） */
data class DetectionRecordEntity(
    val id: Long = 0,
    val riskLevel: String,
    val score: Int,
    val summary: String,
    val reasons: String,       // 原因以 | 分隔
    val senderNumber: String,
    val timestamp: Long,
    val source: String = "RULE"
)
