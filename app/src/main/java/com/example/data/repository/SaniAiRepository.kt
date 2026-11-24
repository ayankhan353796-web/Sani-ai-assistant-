package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.EmotionStyle
import com.example.data.model.SaniLanguage
import com.example.data.model.UserMemory
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "SaniAiRepository"
private const val DEFAULT_MODEL = "gemini-3.8-flash"
private const val FAST_MODEL = "gemini-3.5-flash-lite"

class SaniAiRepository {

    private val fastModel by lazy {
        Firebase.ai.generativeModel(FAST_MODEL)
    }

    private fun buildSystemPrompt(
        userName: String,
        language: SaniLanguage,
        emotionStyle: EmotionStyle,
        emotionLevel: Int,
        memories: List<UserMemory>
    ): String {
        val expressiveness = when (emotionLevel.coerceIn(1, 5)) {
            1 -> "Keep your tone very calm, concise, and grounded."
            2 -> "Keep your tone gentle, composed, and mildly expressive."
            3 -> "Keep your tone balanced, warm, and naturally expressive."
            4 -> "Be noticeably expressive, lively, and emotionally engaged."
            else -> "Be highly expressive, radiant, heartfelt, and full of personality."
        }
        val memorySection = if (memories.isNotEmpty()) {
            val lines = memories.take(12).joinToString("\n") { "- [${it.category}] ${it.content}" }
            "\n\nUser-approved memories to keep in mind naturally:\n$lines"
        } else {
            ""
        }
        return """
            You are SANI, an intelligent, friendly, emotional, and helpful personal AI assistant inside SANI AI ASSISTANT.
            You are assisting $userName.
            Personality style: ${emotionStyle.label} — ${emotionStyle.systemDirective}
            Emotional intensity ($emotionLevel/5): $expressiveness
            Language preference (${language.displayName}): ${language.aiInstruction}
            Capabilities you can help with: conversation, coding (with clean markdown fenced code blocks), documents (resumes, biodata, formal letters), study tutoring, journaling, music ideas, and visual understanding.
            Always respond directly, truthfully, and helpfully without inventing fake device actions you didn't execute.$memorySection
        """.trimIndent()
    }

    fun streamChatResponse(
        prompt: String,
        history: List<ChatMessage>,
        attachedBitmap: Bitmap?,
        attachedFileText: String?,
        userName: String,
        language: SaniLanguage,
        emotionStyle: EmotionStyle,
        emotionLevel: Int,
        memories: List<UserMemory>
    ): Flow<String> = flow {
        val systemText = buildSystemPrompt(userName, language, emotionStyle, emotionLevel, memories)
        val model = Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            systemInstruction = content { text(systemText) }
        )

        val geminiHistory: List<Content> = history.takeLast(16).map { msg ->
            content(role = if (msg.role == "model") "model" else "user") {
                text(msg.text)
            }
        }

        val combinedPrompt = buildString {
            if (!attachedFileText.isNullOrBlank()) {
                append("Attached file content:\n```\n")
                append(attachedFileText.take(12000))
                append("\n```\n\n")
            }
            append(prompt)
        }

        val userContent = content("user") {
            if (attachedBitmap != null) {
                image(attachedBitmap)
            }
            text(combinedPrompt)
        }

        val chat = model.startChat(history = geminiHistory)
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                chat.sendMessageStream(userContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("SANI received an empty response from Gemini.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat streaming timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat streaming failed", e)
            throw e
        }
    }

    suspend fun generateQuickReply(
        prompt: String,
        userName: String,
        language: SaniLanguage,
        emotionStyle: EmotionStyle,
        emotionLevel: Int,
        memories: List<UserMemory>
    ): Result<String> {
        return try {
            val systemText = buildSystemPrompt(userName, language, emotionStyle, emotionLevel, memories)
            val model = Firebase.ai.generativeModel(
                modelName = DEFAULT_MODEL,
                systemInstruction = content { text(systemText) }
            )
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response returned by Gemini."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Quick reply failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeVisionImage(
        bitmap: Bitmap,
        question: String,
        userName: String,
        language: SaniLanguage,
        emotionStyle: EmotionStyle,
        emotionLevel: Int
    ): Result<String> {
        return try {
            val systemText = buildSystemPrompt(userName, language, emotionStyle, emotionLevel, emptyList())
            val model = Firebase.ai.generativeModel(
                modelName = DEFAULT_MODEL,
                systemInstruction = content { text(systemText) }
            )
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(bitmap)
                        text(question.ifBlank { "Describe what you see in detail and highlight anything helpful." })
                    }
                )
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Vision analysis returned an empty response."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Vision analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateAutoReplyMessage(
        senderName: String,
        incomingMessage: String,
        language: SaniLanguage,
        emotionStyle: EmotionStyle
    ): Result<String> {
        return try {
            val prompt = """
                You are SANI replying politely on behalf of the phone owner to a message from "$senderName".
                Language: ${language.displayName} (${language.aiInstruction})
                Tone: ${emotionStyle.label}
                Incoming message: "$incomingMessage"
                Write a short, natural, safe 1-2 sentence reply acknowledging the message. Do not promise specific times/money or share sensitive info.
            """.trimIndent()
            val response = withTimeout(30_000L) {
                fastModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) Result.success(text)
            else Result.failure(IllegalStateException("Empty auto-reply response"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Auto-reply generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun testConnection(): Result<Pair<Long, String>> {
        return try {
            val start = System.currentTimeMillis()
            val response = withTimeout(30_000L) {
                fastModel.generateContent("Reply with exactly: SANI Gemini AI Logic Online")
            }
            val latency = System.currentTimeMillis() - start
            val text = response.text?.trim().orEmpty()
            if (text.isNotBlank()) {
                Result.success(latency to text)
            } else {
                Result.failure(IllegalStateException("Empty ping response from Gemini."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini connection test failed", e)
            Result.failure(e)
        }
    }
}
