package com.example.smsvote.data.model

import java.util.UUID

data class Poll(
    val id: String = UUID.randomUUID().toString(),
    val question: String = "",
    val options: List<String> = emptyList(),
    val maxChoices: Int = 1,
    val endTime: Long? = null,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
