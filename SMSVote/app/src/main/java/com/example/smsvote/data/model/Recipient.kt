package com.example.smsvote.data.model

import java.util.UUID

data class Recipient(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val phoneNumber: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
