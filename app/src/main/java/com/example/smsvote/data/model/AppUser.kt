package com.example.smsvote.data.model

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "operator",
    val status: String = "approved"
) {
    val isSuperAdmin: Boolean get() = role == "super_admin"
    val isAdmin: Boolean get() = role == "admin" || role == "super_admin"
    val isPending: Boolean get() = status == "pending"
    val roleLabel: String get() = when (role) {
        "super_admin" -> "總管理員"
        "admin" -> "管理員"
        else -> "操作員"
    }
}
