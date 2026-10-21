package com.example.smsvote.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class InvitationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val invitations = db.collection("invitations")
    private val users = db.collection("users")

    suspend fun createInvitation(role: String, createdBy: String): String {
        val token = UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
        val now = System.currentTimeMillis()
        invitations.document(token).set(mapOf(
            "token" to token,
            "role" to role,
            "createdBy" to createdBy,
            "createdAt" to now,
            "expiresAt" to now + 48 * 60 * 60 * 1000L,
            "used" to false,
            "usedBy" to null
        )).await()
        return token
    }

    // Returns the granted role string on success, null on failure
    suspend fun redeemInvitation(token: String, uid: String): String? {
        val doc = invitations.document(token).get().await()
        if (!doc.exists()) return null
        if (doc.getBoolean("used") == true) return null
        val expiresAt = doc.getLong("expiresAt") ?: return null
        if (System.currentTimeMillis() > expiresAt) return null
        val role = doc.getString("role") ?: return null

        invitations.document(token).update(mapOf("used" to true, "usedBy" to uid)).await()
        users.document(uid).update(mapOf("role" to role, "status" to "approved")).await()
        return role
    }
}
