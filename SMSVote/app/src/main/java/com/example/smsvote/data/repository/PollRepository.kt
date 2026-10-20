package com.example.smsvote.data.repository

import com.example.smsvote.data.model.Poll
import com.example.smsvote.data.model.VoteRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PollRepository {
    private val db = FirebaseFirestore.getInstance()
    private val polls = db.collection("polls")

    fun getAllPolls(): Flow<List<Poll>> = callbackFlow {
        val listener = polls
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toPoll() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getPollsByCreator(uid: String): Flow<List<Poll>> = callbackFlow {
        val listener = polls
            .whereEqualTo("createdBy", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { it.toPoll() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertPoll(poll: Poll) {
        polls.document(poll.id).set(poll.toMap()).await()
    }

    suspend fun deletePoll(pollId: String) {
        polls.document(pollId).delete().await()
    }

    suspend fun getPollById(pollId: String): Poll? =
        polls.document(pollId).get().await().toPoll()

    suspend fun hasVoted(voterHash: String, pollId: String): Boolean =
        polls.document(pollId).collection("votes").document(voterHash).get().await().exists()

    suspend fun insertVote(voteRecord: VoteRecord) {
        polls.document(voteRecord.pollId)
            .collection("votes")
            .document(voteRecord.voterHash)
            .set(voteRecord.toMap())
            .await()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toPoll(): Poll? {
        if (!exists()) return null
        @Suppress("UNCHECKED_CAST")
        val options = (get("options") as? List<String>)?.filter { it.isNotEmpty() }
            ?: listOfNotNull(getString("optionA"), getString("optionB"), getString("optionC"))
                .filter { it.isNotEmpty() }
        return Poll(
            id = getString("id") ?: id,
            question = getString("question") ?: return null,
            options = options,
            maxChoices = (getLong("maxChoices") ?: 1L).toInt(),
            endTime = getLong("endTime"),
            createdBy = getString("createdBy") ?: "",
            createdAt = getLong("createdAt") ?: 0L,
            isActive = getBoolean("isActive") ?: true
        )
    }

    private fun Poll.toMap() = mapOf(
        "id" to id, "question" to question, "options" to options,
        "maxChoices" to maxChoices, "endTime" to endTime,
        "createdBy" to createdBy, "createdAt" to createdAt, "isActive" to isActive
    )

    private fun VoteRecord.toMap() = mapOf(
        "id" to id, "pollId" to pollId, "voterHash" to voterHash,
        "choices" to choices, "timestamp" to timestamp
    )
}
