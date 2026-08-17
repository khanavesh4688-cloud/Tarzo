package com.tarzo.ai.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.Manifest
import android.os.Build
import android.os.IBinder
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tarzo.ai.MainActivity
import com.tarzo.ai.core.voice.TTSManager
import com.tarzo.ai.receiver.CallStateReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service that detects incoming calls using [TelephonyManager].
 *
 * When a call comes in, uses TTS to announce the caller name
 * (if the contact is known). Provides answer/reject options via
 * notification actions.
 *
 * Requires [Manifest.permission.READ_PHONE_STATE] and
 * [Manifest.permission.READ_CONTACTS].
 */
@AndroidEntryPoint
class CallDetectionService : Service() {

    @Inject lateinit var ttsManager: TTSManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var callStateReceiver: CallStateReceiver? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(CALL_DETECTION_NOTIFICATION_ID, buildNotification("Call detection active"))
        serviceScope.launch { ttsManager.initialize() }
        Log.d(TAG, "CallDetectionService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCallDetection()
            ACTION_STOP -> {
                stopCallDetection()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopCallDetection()
        serviceScope.cancel()
        super.onDestroy()
        Log.d(TAG, "CallDetectionService destroyed")
    }

    // ── Call Detection ────────────────────────────────────────────

    private fun startCallDetection() {
        if (!hasRequiredPermissions()) {
            Log.w(TAG, "Missing required permissions for call detection")
            stopSelf()
            return
        }

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager

        // Register PhoneStateListener for call state changes
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                handleCallState(state, phoneNumber)
            }
        }

        @Suppress("DEPRECATION")
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        // Also register the CallStateReceiver for completeness
        callStateReceiver = CallStateReceiver()
        val filter = IntentFilter().apply {
            addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            addAction(TelecomManager.ACTION_INCOMING_CALL)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(callStateReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(callStateReceiver, filter)
        }

        updateNotification("Call detection active")
        Log.d(TAG, "Call detection started")
    }

    private fun stopCallDetection() {
        @Suppress("DEPRECATION")
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        phoneStateListener = null
        telephonyManager = null

        try {
            callStateReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {
        }
        callStateReceiver = null
    }

    private fun handleCallState(state: Int, phoneNumber: String?) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                val callerName = lookupContactName(phoneNumber)
                val displayName = callerName ?: phoneNumber ?: "Unknown number"
                Log.i(TAG, "Incoming call from: $displayName")
                onIncomingCall(displayName, phoneNumber)
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.d(TAG, "Call ended")
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.d(TAG, "Call in progress")
            }
        }
    }

    private fun onIncomingCall(callerName: String, phoneNumber: String?) {
        // Announce caller via TTS
        serviceScope.launch {
            val announcement = buildAnnouncement(callerName)
            ttsManager.speak(announcement, flush = true)
        }

        // Show notification with answer/reject actions
        showIncomingCallNotification(callerName, phoneNumber)
    }

    private fun buildAnnouncement(callerName: String): String {
        return if (callerName == "Unknown number" || callerName.matches(Regex("\\d+"))) {
            "Unknown number se call aa rahi hai"
        } else {
            "$callerName se call aa rahi hai"
        }
    }

    private fun showIncomingCallNotification(callerName: String, phoneNumber: String?) {
        createNotificationChannel()

        val answerIntent = Intent(this, CallDetectionService::class.java).apply {
            action = ACTION_ANSWER_CALL
            putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
        }
        val answerPendingIntent = PendingIntent.getService(
            this, 10, answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(this, CallDetectionService::class.java).apply {
            action = ACTION_REJECT_CALL
        }
        val rejectPendingIntent = PendingIntent.getService(
            this, 11, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CALL_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Incoming Call")
            .setContentText(callerName)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setTimeoutAfter(30000L)
            .addAction(android.R.drawable.ic_menu_call, "Answer", answerPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reject", rejectPendingIntent)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(INCOMING_CALL_NOTIFICATION_ID, notification)
    }

    private fun lookupContactName(phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) return null

        return try {
            val uri = android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(phoneNumber)
                .build()
            val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to look up contact: ${e.message}")
            null
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return hasPermission(Manifest.permission.READ_PHONE_STATE)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // ── Notification ──────────────────────────────────────────────

    private fun buildNotification(text: String): Notification {
        createNotificationChannel()
        return NotificationCompat.Builder(this, CALL_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("TARZO Call Detection")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(CALL_DETECTION_NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CALL_NOTIFICATION_CHANNEL_ID,
                "TARZO Call Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Incoming call detection and announcement"
                setShowBadge(false)
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "CallDetectionService"
        const val CALL_DETECTION_NOTIFICATION_ID = 1004
        const val INCOMING_CALL_NOTIFICATION_ID = 1005
        const val CALL_NOTIFICATION_CHANNEL_ID = "tarzo_call_detection"

        const val ACTION_START = "com.tarzo.ai.action.START_CALL_DETECTION"
        const val ACTION_STOP = "com.tarzo.ai.action.STOP_CALL_DETECTION"
        const val ACTION_ANSWER_CALL = "com.tarzo.ai.action.ANSWER_CALL"
        const val ACTION_REJECT_CALL = "com.tarzo.ai.action.REJECT_CALL"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"

        fun start(context: Context) {
            val intent = Intent(context, CallDetectionService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallDetectionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
