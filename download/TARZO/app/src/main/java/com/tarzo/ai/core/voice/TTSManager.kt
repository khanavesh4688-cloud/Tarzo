package com.tarzo.ai.core.voice

import android.content.Context
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
    private var isInitialized = false
    private var speechRate = 1.0f
    private var pitch = 1.0f
    private var currentLocale: Locale = Locale("hi", "IN")
    private var utteranceCounter = 0
    private val pendingUtterances = ArrayDeque<String>()
    private val initLatch = kotlinx.coroutines.sync.Mutex()
    private var initDeferred: CompletableDeferred<Boolean>? = null

    suspend fun initialize(): Boolean {
        if (isInitialized) return true
        initDeferred = CompletableDeferred()

        return withContext(Dispatchers.Main) {
            try {
                tts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        isInitialized = true
                        val result = tts?.setLanguage(currentLocale)
                        if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED
                        ) {
                            Log.w(TAG, "Language ${currentLocale.toLanguageTag()} not available, falling back to English")
                            tts?.setLanguage(Locale.ENGLISH)
                        }
                        tts?.setSpeechRate(speechRate)
                        tts?.setPitch(pitch)
                        initDeferred?.complete(true)
                    } else {
                        Log.e(TAG, "TTS initialization failed with status: $status")
                        initDeferred?.complete(false)
                    }
                }
                initDeferred?.await() ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing TTS", e)
                false
            }
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!isInitialized) {
            scope.launch {
                val success = initialize()
                if (success) {
                    speakInternal(text, flush)
                } else {
                    _status.value = TTSStatus(TTSState.ERROR, errorMessage = "TTS not initialized")
                }
            }
            return
        }
        speakInternal(text, flush)
    }

    private fun speakInternal(text: String, flush: Boolean) {
        val safeText = text.take(4000)
        val utteranceId = "tts_${utteranceCounter++}"

        tts?.setSpeechRate(speechRate)
        tts?.setPitch(pitch)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId == utteranceId) {
                    _status.value = TTSStatus(TTSState.SPEAKING, utteranceId)
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == utteranceId) {
                    if (pendingUtterances.isNotEmpty()) {
                        val next = pendingUtterances.removeFirst()
                        speakInternal(next, false)
                    } else {
                        _status.value = TTSStatus(TTSState.IDLE)
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS error for utterance: $utteranceId")
                if (utteranceId == utteranceId) {
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

        val result = tts?.speak(safeText, queueMode, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            _status.value = TTSStatus(TTSState.ERROR, errorMessage = "Failed to enqueue speech")
        }
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
            .replace("kya", "kya")
            .replace("hai", "hai")
            .replace("nahi", "nahi")
            .replace("bhai", "bhai")
    }

    fun stop() {
        pendingUtterances.clear()
        tts?.stop()
        _status.value = TTSStatus(TTSState.IDLE)
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
            Log.w(TAG, "Failed to set language to ${locale.toLanguageTag()}")
            tts?.setLanguage(Locale.ENGLISH)
            currentLocale = Locale.ENGLISH
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

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
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