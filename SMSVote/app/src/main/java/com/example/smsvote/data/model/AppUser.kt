package com.example.smsvote.data.model

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "operator",
    val status: String = "approved"
) {
    val isAdmin: Boolean get() = role == "admin"
    val isPending: Boolean get() = status == "pending"
}
