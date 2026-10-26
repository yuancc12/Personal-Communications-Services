package com.example.smsvote.data.repository

import com.example.smsvote.data.model.AppUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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

    fun getPreApproved(): Flow<List<Pair<String, String>>> = callbackFlow {
        val listener = db.collection("pre_approved")
            .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snapshot?.documents?.map { doc ->
                    (doc.getString("email") ?: doc.id) to (doc.getString("role") ?: "operator")
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun removePreApproved(email: String) {
        db.collection("pre_approved").document(email.lowercase()).delete().await()
    }

    // Returns null on success, error message string on failure
    private suspend fun sendInviteEmail(toEmail: String, roleText: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("service_id", "service_z7q6mq4")
                put("template_id", "template_z4nfj6r")
                put("user_id", "CaJ7sQbJPC3OCcRHw")
                put("accessToken", "Bj93gFxN8GXsCAkBdoJJg")
                put("template_params", JSONObject().apply {
                    put("to_email", toEmail)
                    put("role_text", roleText)
                })
            }.toString().toByteArray()

            val conn = URL("https://api.emailjs.com/api/v1.0/email/send").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true
            conn.outputStream.use { it.write(body) }

            val code = conn.responseCode
            if (code == 200) {
                conn.disconnect()
                null
            } else {
                val errorBody = runCatching {
                    conn.errorStream?.bufferedReader()?.readText()
                }.getOrNull() ?: "（無錯誤訊息）"
                conn.disconnect()
                "EmailJS 錯誤 $code：$errorBody"
            }
        } catch (e: Exception) {
            "寄信例外：${e.message}"
        }
    }

    // Returns email error message if sending failed, null if all succeeded
    suspend fun preApproveEmail(email: String, role: String): String? {
        val emailLower = email.lowercase()

        // Check duplicate
        val existing = db.collection("pre_approved").document(emailLower).get().await()
        if (existing.exists()) {
            val existingRole = existing.getString("role") ?: "operator"
            val label = if (existingRole == "admin") "管理員" else "操作員"
            throw Exception("此 Email 已有授權記錄（$label），如需更改請先移除再重新新增")
        }

        db.collection("pre_approved").document(emailLower).set(mapOf(
            "email" to emailLower,
            "role" to role,
            "addedAt" to System.currentTimeMillis()
        )).await()

        val roleText = if (role == "admin") "管理員" else "操作員"
        return sendInviteEmail(emailLower, roleText)
    }
}
