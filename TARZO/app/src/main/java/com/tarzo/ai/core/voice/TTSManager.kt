package com.tarzo.ai.core.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class TTSState {
    IDLE,
    SPEAKING,
    ERROR
}

data class TTSStatus(
    val state: TTSState = TTSState.IDLE,
    val currentUtteranceId: String? = null,
    val errorMessage: String? = null
)

@Singleton
class TTSManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _status = MutableStateFlow(TTSStatus())
    val status: StateFlow<TTSStatus> = _status.asStateFlow()

    private var tts: TextToSpeech? = null
    @Volatile private var isInitialized = false
    private var speechRate = 1.0f
    private var pitch = 1.0f
    private var currentLocale: Locale = Locale("hi", "IN")
    private var utteranceCounter = 0
    private val pendingUtterances = ArrayDeque<String>()
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private var audioFocusRequest: AudioFocusRequest? = null

    // Single init lock to prevent double initialization
    private val initMutex = kotlinx.coroutines.sync.Mutex()
    private var initJob: Job? = null

    fun initializeAsync() {
        if (initJob?.isActive == true) return
        initJob = scope.launch {
            initialize()
        }
    }

    suspend fun initialize(): Boolean {
        if (isInitialized) return true
        initMutex.lock()
        try {
            if (isInitialized) return true

            return withContext(Dispatchers.Main) {
                try {
                    val initResult = CompletableDeferred<Boolean>()

                    // Shutdown any existing instance first
                    tts?.shutdown()
                    tts = null

                    tts = TextToSpeech(context.applicationContext) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            // Set audio attributes so TTS uses media/music stream
                            tts?.setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .build()
                            )
                            val langResult = tts?.setLanguage(currentLocale)
                            if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                                langResult == TextToSpeech.LANG_NOT_SUPPORTED
                            ) {
                                Log.w(TAG, "Hindi TTS not available, trying English")
                                val enResult = tts?.setLanguage(Locale("en", "IN"))
                                if (enResult == TextToSpeech.LANG_MISSING_DATA ||
                                    enResult == TextToSpeech.LANG_NOT_SUPPORTED
                                ) {
                                    Log.w(TAG, "English-IN TTS not available, trying US English")
                                    tts?.setLanguage(Locale.US)
                                }
                            }
                            tts?.setSpeechRate(speechRate)
                            tts?.setPitch(pitch)
                            isInitialized = true
                            initResult.complete(true)
                            Log.i(TAG, "TTS initialized successfully")
                        } else {
                            Log.e(TAG, "TTS init failed: $status")
                            initResult.complete(false)
                        }
                    }
                    initResult.await()
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing TTS", e)
                    false
                }
            }
        } finally {
            initMutex.unlock()
        }
    }

    /**
     * Non-blocking speak. Queues utterance and returns immediately.
     */
    fun speak(text: String, flush: Boolean = true) {
        if (text.isBlank()) return
        scope.launch {
            if (!isInitialized) {
                val ok = initialize()
                if (!ok) {
                    _status.value = TTSStatus(TTSState.ERROR, errorMessage = "TTS not initialized")
                    return@launch
                }
            }
            speakInternal(text, flush)
        }
    }

    /**
     * Blocking speak - suspends until TTS finishes speaking.
     * Use this when you need to wait for speech to complete.
     */
    suspend fun speakAndWait(text: String, flush: Boolean = true) {
        if (text.isBlank()) return
        if (!isInitialized) {
            val ok = initialize()
            if (!ok) {
                _status.value = TTSStatus(TTSState.ERROR, errorMessage = "TTS not initialized")
                return
            }
        }
        val id = speakInternal(text, flush)
        if (id != null) {
            // Wait until this utterance finishes
            status.first { s ->
                s.state == TTSState.IDLE && s.currentUtteranceId != id
            }
            delay(100) // Small buffer after IDLE
        }
    }

    /**
     * @return The utterance ID used, or null on failure.
     */
    private fun speakInternal(text: String, flush: Boolean): String? {
        val safeText = text.take(4000).trim()
        if (safeText.isEmpty()) return null

        val utteranceId = "tts_${utteranceCounter++}"

        // Request audio focus before speaking
        requestAudioFocus()

        tts?.setSpeechRate(speechRate)
        tts?.setPitch(pitch)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                if (id == utteranceId) {
                    _status.value = TTSStatus(TTSState.SPEAKING, utteranceId)
                    Log.d(TAG, "Started speaking: $utteranceId")
                }
            }

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    Log.d(TAG, "Done speaking: $utteranceId")
                    if (pendingUtterances.isNotEmpty()) {
                        val next = pendingUtterances.removeFirst()
                        speakInternal(next, false)
                    } else {
                        _status.value = TTSStatus(TTSState.IDLE)
                    }
                }
            }

            override fun onError(id: String?) {
                Log.e(TAG, "TTS error for utterance: $id (expected: $utteranceId)")
                if (id == utteranceId) {
                    if (pendingUtterances.isNotEmpty()) {
                        val next = pendingUtterances.removeFirst()
                        speakInternal(next, false)
                    } else {
                        _status.value = TTSStatus(TTSState.ERROR, errorMessage = "Utterance failed")
                        _status.value = TTSStatus(TTSState.IDLE)
                    }
                }
            }
        })

        val queueMode = if (flush) {
            pendingUtterances.clear()
            TextToSpeech.QUEUE_FLUSH
        } else {
            TextToSpeech.QUEUE_ADD
        }

        _status.value = TTSStatus(TTSState.SPEAKING, utteranceId)
        val result = tts?.speak(safeText, queueMode, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "speak() returned ERROR for: $safeText")
            _status.value = TTSStatus(TTSState.ERROR, errorMessage = "Failed to enqueue speech")
            return null
        }
        Log.d(TAG, "Enqueued utterance $utteranceId: ${safeText.take(50)}...")
        return utteranceId
    }

    fun speakHinglish(text: String, flush: Boolean = true) {
        val processed = preprocessHinglish(text)
        speak(processed, flush)
    }

    private fun preprocessHinglish(text: String): String {
        return text
            .replace("₹", "rupaye")
            .replace("%", "percent")
            .replace("&", "aur")
            .replace("@", "at the rate")
            .replace("#", "hash")
    }

    fun stop() {
        pendingUtterances.clear()
        tts?.stop()
        abandonAudioFocus()
        _status.value = TTSStatus(TTSState.IDLE)
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest == null) {
                    audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        .setAcceptsDelayedFocusGain(false)
                        .build()
                }
                audioManager.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request failed: ${e.message}")
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus abandon failed: ${e.message}")
        }
    }

    fun setLanguage(locale: Locale): Boolean {
        if (!isInitialized) return false
        val result = tts?.setLanguage(locale)
        val success = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
        if (success) {
            currentLocale = locale
            Log.d(TAG, "Language set to ${locale.toLanguageTag()}")
        } else {
            Log.w(TAG, "Failed to set language to ${locale.toLanguageTag()}, falling back to en-IN")
            tts?.setLanguage(Locale("en", "IN"))
            currentLocale = Locale("en", "IN")
        }
        return success
    }

    fun setLanguageByTag(languageTag: String): Boolean {
        val locale = when (languageTag) {
            "hi-IN" -> Locale("hi", "IN")
            "en-IN" -> Locale("en", "IN")
            "en-US" -> Locale("en", "US")
            "hi" -> Locale("hi")
            else -> Locale.ENGLISH
        }
        return setLanguage(locale)
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.1f, 3.0f)
        tts?.setSpeechRate(speechRate)
    }

    fun setPitch(newPitch: Float) {
        pitch = newPitch.coerceIn(0.1f, 2.0f)
        tts?.setPitch(pitch)
    }

    fun getCurrentLocale(): Locale = currentLocale

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun isReady(): Boolean = isInitialized

    fun shutdown() {
        stop()
        abandonAudioFocus()
        tts?.shutdown()
        tts = null
        isInitialized = false
        audioFocusRequest = null
    }

    fun getAvailableLanguages(): List<Locale> {
        if (!isInitialized) return emptyList()
        return try {
            tts?.availableLanguages?.toList() ?: listOf(
                Locale("hi", "IN"),
                Locale("en", "IN"),
                Locale.ENGLISH
            )
        } catch (e: Exception) {
            listOf(Locale("hi", "IN"), Locale("en", "IN"), Locale.ENGLISH)
        }
    }

    companion object {
        private const val TAG = "TTSManager"
    }
}
