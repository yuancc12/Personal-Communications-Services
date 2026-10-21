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
            val allUsers = users.get().await()
            val isFirst  = allUsers.isEmpty

            // Check if this email was pre-approved by an admin via the web dashboard
            // Guard: Firestore document ID cannot be empty
            val preApproved = if (email.isNotEmpty()) {
                runCatching {
                    db.collection("pre_approved").document(email.lowercase()).get().await()
                }.getOrNull()
            } else null

            val role = when {
                isFirst                        -> "super_admin"
                preApproved?.exists() == true  -> preApproved.getString("role") ?: "operator"
                else                           -> "operator"
            }
            val status = if (isFirst || preApproved?.exists() == true) "approved" else "pending"

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
                val role = doc.getString("role") ?: "operator"
                AppUser(
                    uid = doc.getString("uid") ?: doc.id,
                    email = doc.getString("email") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    role = role,
                    // super_admin 是第一個帳號，沒有 status 欄位也視為 approved；
                    // 其他缺少 status 欄位的舊帳號一律視為 pending 等待審核
                    status = doc.getString("status") ?: if (role == "super_admin") "approved" else "pending"
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
