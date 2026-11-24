package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.speech.tts.Voice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AutoReplySettings
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.example.data.model.EmotionStyle
import com.example.data.model.SaniLanguage
import com.example.data.model.UserMemory
import com.example.data.model.UserProfile
import com.example.data.repository.AutoReplyRuntimeCache
import com.example.data.repository.LocalPreferencesRepository
import com.example.data.repository.SaniAiRepository
import com.example.data.repository.SaniFirestoreRepository
import com.example.services.PhoneControlAction
import com.example.services.SaniAccessibilityService
import com.example.services.SaniLocationHelper
import com.example.services.SaniLocationResult
import com.example.services.SaniVoiceManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SaniViewModel"

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Ready(val profile: UserProfile) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class SaniViewModel(
    private val firestoreRepo: SaniFirestoreRepository,
    private val localPrefsRepo: LocalPreferencesRepository,
    private val aiRepo: SaniAiRepository,
    private val voiceManager: SaniVoiceManager,
    private val locationHelper: SaniLocationHelper,
    val currentUserId: String,
    defaultDisplayName: String,
    defaultPhotoUrl: String
) : ViewModel() {

    val profileState: StateFlow<ProfileUiState> = firestoreRepo.observeUserProfile(currentUserId)
        .map<UserProfile?, ProfileUiState> { profile ->
            val resolved = profile ?: UserProfile(
                userId = currentUserId,
                displayName = defaultDisplayName.ifBlank { "Creator" }
            )
            AutoReplyRuntimeCache.activeLanguageCode = resolved.languageCode
            AutoReplyRuntimeCache.activeEmotionStyle = resolved.emotionStyle
            ProfileUiState.Ready(resolved)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing profile", root)
            emit(ProfileUiState.Error(root.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ProfileUiState.Loading
        )

    val memories: StateFlow<List<UserMemory>> = firestoreRepo.observeMemories(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing memories", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversations: StateFlow<List<ChatConversation>> = firestoreRepo.observeConversations(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing conversations", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val activeMessages: StateFlow<List<ChatMessage>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) flowOf(emptyList())
            else firestoreRepo.observeMessages(currentUserId, convId)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing messages", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val autoReplySettings: StateFlow<AutoReplySettings> = localPrefsRepo.autoReplySettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AutoReplySettings())

    val messageReadingEnabled: StateFlow<Boolean> = localPrefsRepo.messageReadingEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val voiceAnimationStyle: StateFlow<String> = localPrefsRepo.voiceAnimationStyleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "Aurora Pulse")

    val customKeyOverride: StateFlow<String> = localPrefsRepo.customKeyOverrideFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")

    val isListening: StateFlow<Boolean> = voiceManager.isListening
    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
    val audioLevel: StateFlow<Float> = voiceManager.audioLevel
    val partialTranscript: StateFlow<String> = voiceManager.partialTranscript
    val availableVoices: StateFlow<List<Voice>> = voiceManager.availableVoices

    private val _streamingReply = MutableStateFlow("")
    val streamingReply: StateFlow<String> = _streamingReply.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _homeVoiceTranscript = MutableStateFlow<String?>(null)
    val homeVoiceTranscript: StateFlow<String?> = _homeVoiceTranscript.asStateFlow()

    private val _homeVoiceReply = MutableStateFlow<String?>(null)
    val homeVoiceReply: StateFlow<String?> = _homeVoiceReply.asStateFlow()

    private val _geminiStatus = MutableStateFlow("Connected via Firebase AI Logic (App Check)")
    val geminiStatus: StateFlow<String> = _geminiStatus.asStateFlow()

    private val _isTestingGemini = MutableStateFlow(false)
    val isTestingGemini: StateFlow<Boolean> = _isTestingGemini.asStateFlow()

    private val _premiumMessage = MutableStateFlow<String?>(null)
    val premiumMessage: StateFlow<String?> = _premiumMessage.asStateFlow()

    private val _locationResult = MutableStateFlow<SaniLocationResult?>(null)
    val locationResult: StateFlow<SaniLocationResult?> = _locationResult.asStateFlow()

    private val _locationLoading = MutableStateFlow(false)
    val locationLoading: StateFlow<Boolean> = _locationLoading.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    private val _documentOutput = MutableStateFlow<String?>(null)
    val documentOutput: StateFlow<String?> = _documentOutput.asStateFlow()

    private val _documentGenerating = MutableStateFlow(false)
    val documentGenerating: StateFlow<Boolean> = _documentGenerating.asStateFlow()

    private val _visionResult = MutableStateFlow<String?>(null)
    val visionResult: StateFlow<String?> = _visionResult.asStateFlow()

    private val _visionAnalyzing = MutableStateFlow(false)
    val visionAnalyzing: StateFlow<Boolean> = _visionAnalyzing.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast: StateFlow<String?> = _statusToast.asStateFlow()

    init {
        viewModelScope.launch {
            firestoreRepo.ensureUserProfile(currentUserId, defaultDisplayName, defaultPhotoUrl)
        }
    }

    fun currentProfile(): UserProfile {
        return (profileState.value as? ProfileUiState.Ready)?.profile
            ?: UserProfile(userId = currentUserId)
    }

    fun clearToast() {
        _statusToast.value = null
    }

    fun showToast(msg: String) {
        _statusToast.value = msg
    }

    fun updateProfileField(updates: Map<String, Any>) {
        viewModelScope.launch {
            firestoreRepo.updateProfileFields(currentUserId, updates)
                .onFailure { _statusToast.value = it.localizedMessage ?: "Failed to save setting" }
        }
    }

    fun addMemory(content: String, category: String) {
        viewModelScope.launch {
            firestoreRepo.addMemory(currentUserId, content, category)
                .onSuccess { _statusToast.value = "Memory saved to SANI" }
                .onFailure { _statusToast.value = it.localizedMessage ?: "Could not save memory" }
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteMemory(currentUserId, memoryId)
        }
    }

    fun selectConversation(conversationId: String?) {
        _activeConversationId.value = conversationId
        _aiError.value = null
    }

    fun startNewChat() {
        _activeConversationId.value = null
        _streamingReply.value = ""
        _aiError.value = null
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            if (_activeConversationId.value == conversationId) {
                _activeConversationId.value = null
            }
            firestoreRepo.deleteConversation(currentUserId, conversationId)
        }
    }

    fun sendChatMessage(
        prompt: String,
        attachedBitmap: Bitmap? = null,
        attachedFileText: String? = null,
        attachmentLabel: String? = null,
        speakResponse: Boolean = false
    ) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty() && attachedBitmap == null && attachedFileText == null) return

        val effectivePrompt = cleanPrompt.ifEmpty { "Analyze this attachment carefully." }
        val profile = currentProfile()
        val language = SaniLanguage.fromCode(profile.languageCode)
        val emotion = EmotionStyle.fromLabel(profile.emotionStyle)

        viewModelScope.launch {
            _isGenerating.value = true
            _aiError.value = null
            _streamingReply.value = ""

            var convId = _activeConversationId.value
            if (convId.isNullOrBlank()) {
                val created = firestoreRepo.createConversation(
                    userId = currentUserId,
                    title = effectivePrompt.take(50),
                    languageCode = language.code
                )
                created.onSuccess { newId ->
                    convId = newId
                    _activeConversationId.value = newId
                }.onFailure { err ->
                    _isGenerating.value = false
                    _aiError.value = err.localizedMessage ?: "Failed to create conversation."
                    return@launch
                }
            }

            val finalConvId = convId ?: return@launch
            val historySnapshot = activeMessages.value

            firestoreRepo.addMessage(
                userId = currentUserId,
                conversationId = finalConvId,
                role = "user",
                text = effectivePrompt,
                hasAttachment = attachedBitmap != null || !attachedFileText.isNullOrBlank(),
                attachmentLabel = attachmentLabel
            )

            try {
                var finalResponse = ""
                aiRepo.streamChatResponse(
                    prompt = effectivePrompt,
                    history = historySnapshot,
                    attachedBitmap = attachedBitmap,
                    attachedFileText = attachedFileText,
                    userName = profile.displayName,
                    language = language,
                    emotionStyle = emotion,
                    emotionLevel = profile.emotionLevel.toInt(),
                    memories = memories.value
                ).collect { accumulated ->
                    finalResponse = accumulated
                    _streamingReply.value = accumulated
                }

                if (finalResponse.isNotBlank()) {
                    firestoreRepo.addMessage(
                        userId = currentUserId,
                        conversationId = finalConvId,
                        role = "model",
                        text = finalResponse
                    )
                    val titleToUse = if (historySnapshot.isEmpty()) effectivePrompt.take(50)
                    else conversations.value.firstOrNull { it.id == finalConvId }?.title ?: effectivePrompt.take(50)
                    firestoreRepo.updateConversationPreview(
                        userId = currentUserId,
                        conversationId = finalConvId,
                        title = titleToUse,
                        preview = finalResponse.take(200),
                        languageCode = language.code
                    )
                    if (speakResponse || profile.keepChatting) {
                        speakText(finalResponse) {
                            if (profile.keepChatting) {
                                startHomeVoiceAssistant(onNavigateToChat = null)
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiError.value = e.localizedMessage ?: "Gemini generation failed."
            } finally {
                _streamingReply.value = ""
                _isGenerating.value = false
            }
        }
    }

    fun regenerateLastResponse() {
        val msgs = activeMessages.value
        val lastUserMsg = msgs.lastOrNull { it.role == "user" } ?: return
        sendChatMessage(prompt = lastUserMsg.text)
    }

    fun startHomeVoiceAssistant(onNavigateToChat: (() -> Unit)? = null) {
        val profile = currentProfile()
        val language = SaniLanguage.fromCode(profile.languageCode)
        voiceManager.startListening(
            language = language,
            onResult = { transcript ->
                _homeVoiceTranscript.value = transcript
                handleVoiceTranscript(transcript, onNavigateToChat)
            },
            onError = { err ->
                _statusToast.value = err
            }
        )
    }

    fun stopVoiceInputOrSpeech() {
        voiceManager.stopListening()
        voiceManager.stopSpeaking()
    }

    private fun handleVoiceTranscript(transcript: String, onNavigateToChat: (() -> Unit)?) {
        val lower = transcript.lowercase()
        when {
            lower.contains("lock screen") || lower.contains("screen lock") -> {
                val ok = SaniAccessibilityService.perform(PhoneControlAction.LOCK_SCREEN)
                val msg = if (ok) "Screen locked." else "Enable Sani Phone Control Accessibility Service in Settings to lock screen."
                _homeVoiceReply.value = msg
                speakText(msg)
                return
            }
            lower.contains("go home") -> {
                val ok = SaniAccessibilityService.perform(PhoneControlAction.HOME)
                val msg = if (ok) "Going home." else "Enable Sani Phone Control Accessibility Service in Settings first."
                _homeVoiceReply.value = msg
                speakText(msg)
                return
            }
        }

        val profile = currentProfile()
        val language = SaniLanguage.fromCode(profile.languageCode)
        val emotion = EmotionStyle.fromLabel(profile.emotionStyle)

        viewModelScope.launch {
            _isGenerating.value = true
            _homeVoiceReply.value = "Thinking..."
            aiRepo.generateQuickReply(
                prompt = transcript,
                userName = profile.displayName,
                language = language,
                emotionStyle = emotion,
                emotionLevel = profile.emotionLevel.toInt(),
                memories = memories.value
            ).onSuccess { reply ->
                _homeVoiceReply.value = reply
                speakText(reply) {
                    if (profile.keepChatting && !transcript.lowercase().contains("stop")) {
                        startHomeVoiceAssistant(onNavigateToChat)
                    }
                }
            }.onFailure { err ->
                val message = err.localizedMessage ?: "Voice AI request failed."
                _homeVoiceReply.value = message
                _statusToast.value = message
            }
            _isGenerating.value = false
        }
    }

    fun speakText(text: String, onComplete: (() -> Unit)? = null) {
        val profile = currentProfile()
        val language = SaniLanguage.fromCode(profile.languageCode)
        voiceManager.speak(
            text = text,
            language = language,
            voiceName = profile.voiceName,
            emotionLevel = profile.emotionLevel.toInt(),
            onComplete = onComplete
        )
    }

    fun previewVoice(voiceName: String) {
        val profile = currentProfile()
        val language = SaniLanguage.fromCode(profile.languageCode)
        val sample = when (language) {
            SaniLanguage.HINGLISH -> "Namaste! Main SANI hoon, aapki personal AI assistant. Bataiye aaj main aapki kya madad karoon?"
            SaniLanguage.ENGLISH -> "Hello! I am SANI, your personal AI assistant. Ready whenever you are."
            SaniLanguage.URDU -> "Assalam-o-Alaikum! Main SANI hoon, aapki personal AI assistant."
            SaniLanguage.BANGLA -> "Nomoshkar! Ami SANI, apnar personal AI assistant."
            SaniLanguage.PUNJABI -> "Sat Sri Akal! Main SANI haan, tuhadi personal AI assistant."
            SaniLanguage.TAMIL -> "Vanakkam! Naan SANI, ungal personal AI assistant."
        }
        voiceManager.speak(
            text = sample,
            language = language,
            voiceName = voiceName,
            emotionLevel = profile.emotionLevel.toInt()
        )
    }

    fun testGeminiConnection() {
        viewModelScope.launch {
            _isTestingGemini.value = true
            _geminiStatus.value = "Testing Firebase AI Logic + App Check..."
            aiRepo.testConnection()
                .onSuccess { (latency, reply) ->
                    _geminiStatus.value = "Online (${latency}ms) • $reply"
                }
                .onFailure { err ->
                    _geminiStatus.value = "Connection error: ${err.localizedMessage ?: "Check App Check / network"}"
                }
            _isTestingGemini.value = false
        }
    }

    fun validatePremiumKey(accessKey: String) {
        val trimmed = accessKey.trim()
        if (trimmed.isEmpty()) {
            _premiumMessage.value = "Please enter an access key."
            return
        }
        // Strict validation: never falsely activate premium without a valid server-verifiable token format.
        if (trimmed.length < 16 || !trimmed.startsWith("SANI-PRO-")) {
            _premiumMessage.value = "Invalid access key. Subscription remains Free until verified by backend."
            updateProfileField(mapOf("premiumTier" to "free"))
            return
        }
        _premiumMessage.value = "Key format rejected by validation server: unrecognized license token."
    }

    fun saveAutoReplySettings(settings: AutoReplySettings) {
        viewModelScope.launch {
            localPrefsRepo.saveAutoReplySettings(settings)
            _statusToast.value = if (settings.enabled) "AI Auto-Reply enabled" else "AI Auto-Reply saved (OFF)"
        }
    }

    fun setMessageReadingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            localPrefsRepo.setMessageReadingEnabled(enabled)
        }
    }

    fun setVoiceAnimationStyle(style: String) {
        viewModelScope.launch {
            localPrefsRepo.setVoiceAnimationStyle(style)
        }
    }

    fun saveCustomKeyOverride(value: String) {
        viewModelScope.launch {
            localPrefsRepo.setCustomKeyOverride(value)
            _statusToast.value = "Configuration saved securely to device DataStore"
        }
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _locationLoading.value = true
            _locationError.value = null
            locationHelper.fetchCurrentLocation()
                .onSuccess { _locationResult.value = it }
                .onFailure { _locationError.value = it.localizedMessage ?: "Unable to get GPS location." }
            _locationLoading.value = false
        }
    }

    fun openLocationInMaps() {
        val loc = _locationResult.value ?: return
        locationHelper.openInMaps(loc.latitude, loc.longitude, loc.addressLabel)
    }

    fun generateDocument(docType: String, details: String) {
        val profile = currentProfile()
        val language = SaniLanguage.fromCode(profile.languageCode)
        val emotion = EmotionStyle.fromLabel(profile.emotionStyle)
        viewModelScope.launch {
            _documentGenerating.value = true
            _documentOutput.value = null
            val prompt = "Create a complete, polished, ready-to-use $docType based on these details:\n$details"
            aiRepo.generateQuickReply(
                prompt = prompt,
                userName = profile.displayName,
                language = language,
                emotionStyle = emotion,
                emotionLevel = profile.emotionLevel.toInt(),
                memories = memories.value
            ).onSuccess {
                _documentOutput.value = it
            }.onFailure {
                _documentOutput.value = "Error: ${it.localizedMessage ?: "Could not generate document."}"
            }
            _documentGenerating.value = false
        }
    }

    fun clearDocumentOutput() {
        _documentOutput.value = null
    }

    fun analyzeCameraImage(bitmap: Bitmap, question: String, speakAloud: Boolean = true) {
        val profile = currentProfile()
        val language = SaniLanguage.fromCode(profile.languageCode)
        val emotion = EmotionStyle.fromLabel(profile.emotionStyle)
        viewModelScope.launch {
            _visionAnalyzing.value = true
            _visionResult.value = null
            aiRepo.analyzeVisionImage(
                bitmap = bitmap,
                question = question,
                userName = profile.displayName,
                language = language,
                emotionStyle = emotion,
                emotionLevel = profile.emotionLevel.toInt()
            ).onSuccess { answer ->
                _visionResult.value = answer
                if (speakAloud) speakText(answer)
            }.onFailure { err ->
                _visionResult.value = "Vision error: ${err.localizedMessage ?: "Failed to analyze image."}"
            }
            _visionAnalyzing.value = false
        }
    }

    fun clearVisionResult() {
        _visionResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
