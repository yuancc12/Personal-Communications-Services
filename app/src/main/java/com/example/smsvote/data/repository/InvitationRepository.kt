package com.example.smsvote.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class InvitationRecord(
    val token: String,
    val role: String,
    val createdAt: Long,
    val expiresAt: Long,
    val used: Boolean
)

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

    fun getInvitations(createdBy: String): Flow<List<InvitationRecord>> = callbackFlow {
        val listener = invitations
            .whereEqualTo("createdBy", createdBy)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    InvitationRecord(
                        token = doc.getString("token") ?: doc.id,
                        role = doc.getString("role") ?: "operator",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        expiresAt = doc.getLong("expiresAt") ?: 0L,
                        used = doc.getBoolean("used") ?: false
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
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
