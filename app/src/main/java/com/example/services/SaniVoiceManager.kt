package com.example.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.model.SaniLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

private const val TAG = "SaniVoiceManager"

class SaniVoiceManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = true
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                            _audioLevel.value = 0f
                            onSpeakCompleteCallback?.invoke()
                            onSpeakCompleteCallback = null
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                            _audioLevel.value = 0f
                            onSpeakCompleteCallback = null
                        }
                    })
                    refreshVoices()
                } else {
                    Log.w(TAG, "TTS initialization failed with status=$status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create TextToSpeech", e)
        }
    }

    private var onSpeakCompleteCallback: (() -> Unit)? = null

    fun refreshVoices() {
        if (!ttsReady) return
        try {
            val voices = tts?.voices?.sortedBy { it.name } ?: emptyList()
            _availableVoices.value = voices
        } catch (e: Exception) {
            Log.w(TAG, "Unable to list TTS voices: ${e.message}")
        }
    }

    fun speak(
        text: String,
        language: SaniLanguage,
        voiceName: String = "default",
        emotionLevel: Int = 3,
        onComplete: (() -> Unit)? = null
    ) {
        if (!ttsReady || text.isBlank()) {
            onComplete?.invoke()
            return
        }
        val cleanSpeechText = text
            .replace(Regex("```[\\s\\S]*?```"), " code snippet ")
            .replace(Regex("[*_`#>]"), "")
            .take(3500)

        try {
            val locale = Locale.forLanguageTag(language.ttsTag)
            tts?.language = locale
            if (voiceName != "default") {
                _availableVoices.value.firstOrNull { it.name == voiceName }?.let { selected ->
                    tts?.voice = selected
                }
            }
            val pitch = when (emotionLevel.coerceIn(1, 5)) {
                1 -> 0.94f
                2 -> 0.98f
                3 -> 1.03f
                4 -> 1.08f
                else -> 1.12f
            }
            val rate = when (emotionLevel.coerceIn(1, 5)) {
                1 -> 0.94f
                2 -> 0.98f
                else -> 1.02f
            }
            tts?.setPitch(pitch)
            tts?.setSpeechRate(rate)
            onSpeakCompleteCallback = onComplete
            val utteranceId = UUID.randomUUID().toString()
            tts?.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak failed", e)
            _isSpeaking.value = false
            onComplete?.invoke()
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
        _audioLevel.value = 0f
        onSpeakCompleteCallback = null
    }

    fun startListening(
        language: SaniLanguage,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        stopSpeaking()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition service is not available on this device.")
            return
        }
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _partialTranscript.value = ""
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _audioLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                    _audioLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _audioLevel.value = 0f
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network speech error."
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Tap to speak again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                        else -> "Voice input error ($error)."
                    }
                    onError(message)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _audioLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim().orEmpty()
                    if (best.isNotEmpty()) {
                        _partialTranscript.value = best
                        onResult(best)
                    } else {
                        onError("No speech recognized.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.ttsTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            _isListening.value = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _audioLevel.value = 0f
            Log.e(TAG, "startListening failed", e)
            onError(e.localizedMessage ?: "Failed to start voice input.")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        _isListening.value = false
        _audioLevel.value = 0f
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }
}
