package com.tarzo.ai.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tarzo.ai.MainActivity
import com.tarzo.ai.TarzoApp
import com.tarzo.ai.core.voice.TTSManager
import com.tarzo.ai.core.voice.WakeWordEngine
import com.tarzo.ai.core.voice.WakeWordEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lightweight foreground service that ONLY runs the wake word engine.
 *
 * When "Bolo TARZO" is detected, this service starts [VoiceAssistantService]
 * and sends the wake word detected action to trigger speech recognition.
 *
 * This service can be toggled from settings via [TarzoApp.KEY_WAKE_WORD_ENABLED].
 * It maintains a low-priority notification indicating the wake word listener is active.
 */
@AndroidEntryPoint
class WakeWordService : Service() {

    @Inject lateinit var wakeWordEngine: WakeWordEngine
    @Inject lateinit var ttsManager: TTSManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeWordJob: Job? = null

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(WAKE_WORD_NOTIFICATION_ID, buildNotification())
        Log.d(TAG, "WakeWordService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startWakeWordListening()
            ACTION_STOP -> {
                stopWakeWordListening()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopWakeWordListening()
        serviceScope.cancel()
        super.onDestroy()
        Log.d(TAG, "WakeWordService destroyed")
    }

    // ── Wake Word Detection ───────────────────────────────────────

    private fun startWakeWordListening() {
        if (_isActive.value) return
        _isActive.value = true
        updateNotification("Wake word active — say \"Bolo TARZO\"")

        serviceScope.launch {
            ttsManager.initialize()
        }

        wakeWordJob = serviceScope.launch {
            wakeWordEngine.start()
            wakeWordEngine.events.collect { event ->
                when (event) {
                    is WakeWordEvent.Detected -> {
                        Log.i(TAG, "Wake word 'Bolo TARZO' detected!")
                        onWakeWordDetected()
                    }
                    is WakeWordEvent.Error -> {
                        Log.e(TAG, "Wake word engine error: ${event.message}")
                        _isActive.value = false
                        updateNotification("Wake word error: ${event.message}")
                    }
                    is WakeWordEvent.Listening -> {
                        Log.d(TAG, "Wake word engine listening")
                    }
                    is WakeWordEvent.Stopped -> {
                        Log.d(TAG, "Wake word engine stopped")
                        _isActive.value = false
                    }
                    is WakeWordEvent.AudioLevel -> {
                        // Audio level for visualization
                    }
                }
            }
        }
    }

    private fun stopWakeWordListening() {
        wakeWordJob?.cancel()
        wakeWordJob = null
        wakeWordEngine.stop()
        _isActive.value = false
    }

    private fun onWakeWordDetected() {
        ttsManager.stop()

        // Start VoiceAssistantService which will handle the speech recognition pipeline
        val assistantIntent = VoiceAssistantService.startIntent(this).apply {
            action = VoiceAssistantService.ACTION_WAKE_WORD_DETECTED
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(assistantIntent)
        } else {
            startService(assistantIntent)
        }

        // Play a short acknowledgment sound via TTS
        ttsManager.speak("Ji boliye", flush = true)

        // Restart wake word detection after a brief pause
        // to avoid re-triggering on the same audio
        serviceScope.launch {
            kotlinx.coroutines.delay(1500)
            if (_isActive.value) {
                wakeWordEngine.start()
            }
        }
    }

    // ── Notification ──────────────────────────────────────────────

    private fun buildNotification(): Notification {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WakeWordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("TARZO Wake Word")
            .setContentText("Say \"Bolo TARZO\" to activate")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Disable", stopPendingIntent)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("TARZO Wake Word")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(WAKE_WORD_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "TARZO Wake Word",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Wake word detection notification"
                setShowBadge(false)
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "WakeWordService"
        const val WAKE_WORD_NOTIFICATION_ID = 1003
        const val NOTIFICATION_CHANNEL_ID = "tarzo_wake_word"
        const val ACTION_START = "com.tarzo.ai.action.START_WAKE_WORD"
        const val ACTION_STOP = "com.tarzo.ai.action.STOP_WAKE_WORD"

        /**
         * Starts the wake word service.
         */
        fun start(context: Context) {
            val intent = Intent(context, WakeWordService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stops the wake word service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, WakeWordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
