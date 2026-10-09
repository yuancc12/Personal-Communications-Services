package com.example.personal_communications_services.data

/** 黑名單資料模型 */
data class BlacklistEntity(
    val phoneNumber: String,
    val addedAt: Long = System.currentTimeMillis(),
    val note: String = ""
)
