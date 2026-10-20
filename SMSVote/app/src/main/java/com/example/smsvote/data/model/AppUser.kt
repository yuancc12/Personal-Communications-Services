package com.example.smsvote.data.model

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "operator"
) {
    val isAdmin: Boolean get() = role == "admin"
}
