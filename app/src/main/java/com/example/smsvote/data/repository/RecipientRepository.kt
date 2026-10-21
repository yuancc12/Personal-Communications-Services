package com.example.smsvote.data.repository

import com.example.smsvote.data.model.Recipient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RecipientRepository {
    private val collection = FirebaseFirestore.getInstance().collection("recipients")

    fun getRecipients(): Flow<List<Recipient>> = callbackFlow {
        val listener = collection
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    Recipient(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        phoneNumber = doc.getString("phoneNumber") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addRecipient(recipient: Recipient) {
        collection.document(recipient.id).set(
            mapOf(
                "id" to recipient.id,
                "name" to recipient.name,
                "phoneNumber" to recipient.phoneNumber,
                "createdAt" to recipient.createdAt
            )
        ).await()
    }

    suspend fun deleteRecipient(id: String) {
        collection.document(id).delete().await()
    }
}
