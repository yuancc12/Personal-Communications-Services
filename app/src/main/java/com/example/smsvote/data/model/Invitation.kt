package com.example.smsvote.data.model

data class Invitation(
    val token: String = "",
    val role: String = "operator",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val used: Boolean = false,
    val usedBy: String? = null
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt
    val roleLabel: String get() = if (role == "admin" || role == "super_admin") "管理員" else "操作員"
}
