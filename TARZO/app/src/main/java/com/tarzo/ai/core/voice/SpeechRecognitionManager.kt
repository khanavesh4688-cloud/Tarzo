package com.tarzo.ai.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class SpeechState {
    data object Idle : SpeechState()
    data object Listening : SpeechState()
    data class PartialResult(val text: String) : SpeechState()
    data class FinalResult(val text: String) : SpeechState()
    data class Error(val code: Int, val message: String) : SpeechState()
}

@Singleton
class SpeechRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    @Volatile
    private var isListening = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentLanguage: String = DEFAULT_LANGUAGE
    private var restartAttempts = 0
    private val maxRestartAttempts = 3

    fun startListening(language: String = currentLanguage) {
        if (isListening) {
            stopListening()
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = SpeechState.Error(
                ERROR_NOT_AVAILABLE,
                "Speech recognition not available on this device"
            )
            return
        }

        currentLanguage = language
        isListening = true
        restartAttempts = 0
        createAndStartRecognizer()
    }

    private fun createAndStartRecognizer() {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_DEFAULT_LANGUAGE, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    2000L
                )
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                    500L
                )
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    1500L
                )
            }

            _state.value = SpeechState.Listening
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _state.value = SpeechState.Error(ERROR_INIT_FAILED, "Failed to start: ${e.message}")
            isListening = false
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SpeechState.Listening
            }

            override fun onBeginningOfSpeech() {
                restartAttempts = 0
            }

            override fun onRmsChanged(rmsdB: Float) {
            }

            override fun onBufferReceived(buffer: ByteArray?) {
            }

            override fun onEndOfSpeech() {
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected, try again"
                    else -> "Unknown error ($error)"
                }

                Log.e(TAG, "Speech recognition error: $errorMessage (code: $error)")

                if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                ) {
                    if (restartAttempts < maxRestartAttempts && isListening) {
                        restartAttempts++
                        Log.d(TAG, "Restarting listener, attempt $restartAttempts")
                        speechRecognizer?.destroy()
                        delayAndRestart()
                    } else {
                        _state.value = SpeechState.Error(error, errorMessage)
                        isListening = false
                        cleanup()
                    }
                } else {
                    _state.value = SpeechState.Error(error, errorMessage)
                    isListening = false
                    cleanup()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: emptyList()

                if (matches.isNotEmpty()) {
                    val bestResult = matches.first()
                    Log.d(TAG, "Final result: $bestResult (alternatives: ${matches.drop(1)})")
                    _state.value = SpeechState.FinalResult(bestResult)
                } else {
                    _state.value = SpeechState.Error(ERROR_NO_RESULTS, "No results found")
                }
                isListening = false
                cleanup()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: emptyList()

                if (matches.isNotEmpty()) {
                    val partialText = matches.first()
                    Log.d(TAG, "Partial result: $partialText")
                    _state.value = SpeechState.PartialResult(partialText)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
            }
        }
    }

    private fun delayAndRestart() {
        scope.launch {
            delay(500)
            if (isListening) {
                createAndStartRecognizer()
            }
        }
    }

    fun stopListening() {
        isListening = false
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        cleanup()
        _state.value = SpeechState.Idle
    }

    fun cancelListening() {
        isListening = false
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {
        }
        cleanup()
        _state.value = SpeechState.Idle
    }

    private fun cleanup() {
        try {
            speechRecognizer?.setRecognitionListener(null)
        } catch (_: Exception) {
        }
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    fun setLanguage(language: String) {
        currentLanguage = language
    }

    fun getSupportedLanguages(): List<Locale> {
        return listOf(
            Locale("hi", "IN"),
            Locale("en", "IN"),
            Locale("en", "US"),
            Locale("hi"),
            Locale.ENGLISH,
            Locale.HINDI
        ).distinctBy { it.toLanguageTag() }
    }

    fun isListeningActive(): Boolean = isListening

    fun resetState() {
        _state.value = SpeechState.Idle
    }

    companion object {
        private const val TAG = "SpeechRecognition"
        private const val DEFAULT_LANGUAGE = "hi-IN"
        const val ERROR_NOT_AVAILABLE = 1001
        const val ERROR_INIT_FAILED = 1002
        const val ERROR_NO_RESULTS = 1003
    }
}