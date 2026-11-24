package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.example.data.model.UserMemory
import com.example.data.model.UserProfile
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SaniFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        defaultName: String,
        defaultPhotoUrl: String
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val profile = UserProfile(
                    userId = uid,
                    displayName = defaultName.ifBlank { "Creator" }.take(60),
                    photoUrl = defaultPhotoUrl.take(500)
                )
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfileFields(userId: String, updates: Map<String, Any>): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val allowedUpdates = updates.toMutableMap()
            allowedUpdates["updatedAt"] = FieldValue.serverTimestamp()
            db.collection("users").document(uid).update(allowedUpdates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeMemories(userId: String): Flow<List<UserMemory>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("users").document(userId).collection("memories")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserMemory::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun addMemory(userId: String, content: String, category: String): Result<Unit> {
        val path = "users/$userId/memories"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val trimmed = content.trim().take(500)
            require(trimmed.isNotEmpty()) { "Memory cannot be empty." }
            val cat = category.trim().ifBlank { "Personal" }.take(40)
            val docRef = db.collection("users").document(uid).collection("memories").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "content" to trimmed,
                    "category" to cat,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(userId: String, memoryId: String): Result<Unit> {
        val path = "users/$userId/memories/$memoryId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(memoryId.isNotBlank()) { "Memory ID required." }
            db.collection("users").document(uid).collection("memories").document(memoryId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeConversations(userId: String): Flow<List<ChatConversation>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("users").document(userId).collection("conversations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatConversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun createConversation(
        userId: String,
        title: String,
        languageCode: String
    ): Result<String> {
        val path = "users/$userId/conversations"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid).collection("conversations").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to title.trim().ifBlank { "New Chat" }.take(120),
                    "lastMessagePreview" to "",
                    "languageCode" to languageCode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateConversationPreview(
        userId: String,
        conversationId: String,
        title: String,
        preview: String,
        languageCode: String
    ): Result<Unit> {
        val path = "users/$userId/conversations/$conversationId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            db.collection("users").document(uid).collection("conversations").document(conversationId)
                .update(
                    mapOf(
                        "title" to title.trim().ifBlank { "Chat" }.take(120),
                        "lastMessagePreview" to preview.trim().take(300),
                        "languageCode" to languageCode,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(userId: String, conversationId: String): Result<Unit> {
        val path = "users/$userId/conversations/$conversationId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val convRef = db.collection("users").document(uid)
                .collection("conversations").document(conversationId)
            val msgs = convRef.collection("messages").get().await()
            if (!msgs.isEmpty) {
                val batch = db.batch()
                msgs.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            convRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessage>> = flow {
        if (userId.isBlank() || conversationId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("users").document(userId)
                .collection("conversations").document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
        )
    }

    suspend fun addMessage(
        userId: String,
        conversationId: String,
        role: String,
        text: String,
        hasAttachment: Boolean = false,
        attachmentLabel: String? = null
    ): Result<String> {
        val path = "users/$userId/conversations/$conversationId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val cleanText = text.trim().take(20000)
            require(cleanText.isNotEmpty()) { "Message text cannot be empty." }
            val docRef = db.collection("users").document(uid)
                .collection("conversations").document(conversationId)
                .collection("messages").document()
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "role" to role,
                "text" to cleanText,
                "hasAttachment" to hasAttachment,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!attachmentLabel.isNullOrBlank()) {
                payload["attachmentLabel"] = attachmentLabel.trim().take(120)
            }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
