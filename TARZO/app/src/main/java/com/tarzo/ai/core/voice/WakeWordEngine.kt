package com.tarzo.ai.core.voice

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

sealed class WakeWordEvent {
    data object Detected : WakeWordEvent()
    data class Error(val message: String) : WakeWordEvent()
    data class AudioLevel(val level: Float) : WakeWordEvent()
    data object Listening : WakeWordEvent()
    data object Stopped : WakeWordEvent()
}

@Singleton
class WakeWordEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableStateFlow<WakeWordEvent>(WakeWordEvent.Stopped)
    val events: StateFlow<WakeWordEvent> = _events.asStateFlow()

    @Volatile
    private var isListening = false
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private val wakeWordPhrases = listOf(
        "bolo tarzo", "bolo tarjo", "bolo tarjo", "bolo tarzoo",
        "hey tarzo", "hay tarzo", "hello tarzo", "ok tarzo",
        "bolo tarzo", "bole tarzo", "bol tarzo"
    )

    private val phoneticWakeWords = listOf(
        "b o l o   t a r z o",
        "b o l o   t a r z o",
        "b o l   t a r z o"
    )

    private var energyThreshold = 1500.0
    private val silenceTimeoutMs = 500L
    private var lastVoiceActivityMs = 0L
    private val ringBufferSize = sampleRate / 2
    private val ringBuffer = ArrayDeque<Float>(ringBufferSize)
    private val candidateBuffer = StringBuilder()
    private var candidateStartTimeMs = 0L
    private val candidateTimeoutMs = 2500L

    fun start() {
        if (isListening) return
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            _events.value = WakeWordEvent.Error("Microphone permission not granted")
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _events.value = WakeWordEvent.Error("Failed to initialize AudioRecord")
                audioRecord = null
                return
            }

            isListening = true
            audioRecord?.startRecording()
            _events.value = WakeWordEvent.Listening
            lastVoiceActivityMs = System.currentTimeMillis()

            recordingJob = scope.launch {
                val buffer = ShortArray(bufferSize)
                while (isActive && isListening) {
                    val readCount = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                    if (readCount > 0) {
                        processAudioFrame(buffer, readCount)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting wake word engine", e)
            _events.value = WakeWordEvent.Error("Failed to start: ${e.message}")
            stop()
        }
    }

    fun stop() {
        isListening = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
        } catch (_: IllegalStateException) {
        }
        audioRecord?.release()
        audioRecord = null
        ringBuffer.clear()
        candidateBuffer.clear()
        _events.value = WakeWordEvent.Stopped
    }

    private fun processAudioFrame(buffer: ShortArray, readCount: Int) {
        val energy = calculateEnergy(buffer, readCount)
        val normalizedEnergy = (energy / 32767.0).toFloat()
        _events.value = WakeWordEvent.AudioLevel(normalizedEnergy)

        val currentTime = System.currentTimeMillis()
        val isSpeech = energy > energyThreshold

        if (isSpeech) {
            lastVoiceActivityMs = currentTime
            val spectralFeatures = extractSpectralFeatures(buffer, readCount)
            for (feature in spectralFeatures) {
                ringBuffer.addLast(feature)
            }

            val phoneticToken = matchPhoneticApproximation(buffer, readCount)
            if (phoneticToken != null) {
                if (candidateBuffer.isEmpty()) {
                    candidateStartTimeMs = currentTime
                }
                candidateBuffer.append(" ").append(phoneticToken)

                val candidate = candidateBuffer.toString().trim().lowercase()
                if (checkWakeWordMatch(candidate)) {
                    Log.i(TAG, "Wake word detected: $candidate")
                    _events.value = WakeWordEvent.Detected
                    candidateBuffer.clear()
                    ringBuffer.clear()
                } else if (currentTime - candidateStartTimeMs > candidateTimeoutMs) {
                    candidateBuffer.clear()
                }
            } else if (currentTime - lastVoiceActivityMs > silenceTimeoutMs) {
                candidateBuffer.clear()
            }
        } else {
            if (currentTime - lastVoiceActivityMs > silenceTimeoutMs) {
                candidateBuffer.clear()
                ringBuffer.clear()
            }
        }
    }

    private fun calculateEnergy(buffer: ShortArray, readCount: Int): Double {
        var sum = 0.0
        for (i in 0 until readCount) {
            sum += buffer[i].toDouble() * buffer[i].toDouble()
        }
        return sqrt(sum / readCount)
    }

    private fun extractSpectralFeatures(buffer: ShortArray, readCount: Int): List<Float> {
        val features = mutableListOf<Float>()
        val frameSize = 256
        var idx = 0
        while (idx + frameSize <= readCount) {
            var sumSquares = 0.0
            var sumAbs = 0.0
            var zeroCrossings = 0
            for (i in idx until idx + frameSize) {
                val sample = buffer[i].toFloat()
                sumSquares += sample * sample
                sumAbs += abs(sample)
                if (i > idx && ((buffer[i] >= 0) != (buffer[i - 1] >= 0))) {
                    zeroCrossings++
                }
            }
            val rms = sqrt(sumSquares / frameSize)
            val zcr = zeroCrossings.toFloat() / frameSize
            features.add(rms)
            features.add(zcr)
            idx += frameSize / 2
        }
        return features
    }

    private fun matchPhoneticApproximation(buffer: ShortArray, readCount: Int): String? {
        val energy = calculateEnergy(buffer, readCount)
        if (energy < energyThreshold) return null

        var zeroCrossings = 0
        for (i in 1 until readCount) {
            if ((buffer[i] >= 0) != (buffer[i - 1] >= 0)) zeroCrossings++
        }
        val zcr = zeroCrossings.toFloat() / readCount

        var sumSquares = 0.0
        for (i in 0 until readCount) sumSquares += buffer[i].toDouble() * buffer[i].toDouble()
        val rms = sqrt(sumSquares / readCount)

        return when {
            zcr < 0.05 && rms > energyThreshold * 0.5 -> "b"
            zcr in 0.05..0.15 && rms > energyThreshold * 0.3 -> "o"
            zcr in 0.15..0.25 && rms > energyThreshold * 0.4 -> "l"
            zcr in 0.25..0.35 -> "t"
            zcr in 0.35..0.45 -> "a"
            zcr in 0.45..0.55 -> "r"
            zcr > 0.55 -> "z"
            else -> null
        }
    }

    private fun checkWakeWordMatch(candidate: String): Boolean {
        for (phrase in wakeWordPhrases) {
            val similarity = levenshteinSimilarity(candidate, phrase)
            if (similarity > 0.65) {
                return true
            }
        }
        for (phonetic in phoneticWakeWords) {
            val cleaned = phonetic.replace(" ", "")
            val candidateCleaned = candidate.replace(" ", "")
            if (cleaned.contains(candidateCleaned) || candidateCleaned.contains(cleaned)) {
                return true
            }
        }
        return false
    }

    private fun levenshteinSimilarity(a: String, b: String): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val aLower = a.lowercase().trim()
        val bLower = b.lowercase().trim()
        val maxLen = maxOf(aLower.length, bLower.length)
        val distance = levenshteinDistance(aLower, bLower)
        return 1.0f - (distance.toFloat() / maxLen.toFloat())
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }

    fun setEnergyThreshold(threshold: Double) {
        energyThreshold = threshold
    }

    fun isRunning(): Boolean = isListening

    companion object {
        private const val TAG = "WakeWordEngine"
    }
}