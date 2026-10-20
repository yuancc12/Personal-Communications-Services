package com.example.smsvote.data.repository

import com.example.smsvote.data.model.AppUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val users = db.collection("users")

    suspend fun getOrCreateUser(uid: String, email: String, displayName: String): AppUser {
        val doc = users.document(uid).get().await()
        return if (doc.exists()) {
            AppUser(
                uid = uid,
                email = doc.getString("email") ?: email,
                displayName = doc.getString("displayName") ?: displayName,
                role = doc.getString("role") ?: "operator",
                status = doc.getString("status") ?: "approved"
            )
        } else {
            // First user ever becomes admin+approved; all others become operator+pending
            val allUsers = users.get().await()
            val isFirst = allUsers.isEmpty
            val role = if (isFirst) "admin" else "operator"
            val status = if (isFirst) "approved" else "pending"
            val newUser = AppUser(uid = uid, email = email, displayName = displayName, role = role, status = status)
            users.document(uid).set(mapOf(
                "uid" to uid, "email" to email, "displayName" to displayName,
                "role" to role, "status" to status
            )).await()
            newUser
        }
    }

    fun getAllUsers(): Flow<List<AppUser>> = callbackFlow {
        val listener = users.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val list = snapshot?.documents?.mapNotNull { doc ->
                AppUser(
                    uid = doc.getString("uid") ?: doc.id,
                    email = doc.getString("email") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    role = doc.getString("role") ?: "operator"
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateRole(uid: String, newRole: String) {
        users.document(uid).update("role", newRole).await()
    }

    suspend fun approveUser(uid: String) {
        users.document(uid).update("status", "approved").await()
    }

    suspend fun rejectUser(uid: String) {
        users.document(uid).delete().await()
    }
}
