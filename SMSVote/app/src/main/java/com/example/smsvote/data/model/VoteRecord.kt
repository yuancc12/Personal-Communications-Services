package com.example.smsvote.data.model

import java.util.UUID

data class VoteRecord(
    val id: String = UUID.randomUUID().toString(),
    val pollId: String = "",
    val voterHash: String = "",
    val choices: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
