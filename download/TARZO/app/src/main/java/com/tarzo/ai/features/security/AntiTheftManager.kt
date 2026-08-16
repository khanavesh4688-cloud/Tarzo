package com.tarzo.ai.features.security

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.tarzo.ai.core.storage.SecureStorage
import com.tarzo.ai.util.Constants.ANTI_THEFT_ALARM_DURATION_MS
import com.tarzo.ai.util.Constants.ANTI_THEFT_NOTIFICATION_ID
import com.tarzo.ai.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages anti-theft features including:
 * - Movement detection via accelerometer sensor
 * - Charger disconnect alerts via BroadcastReceiver
 * - Intruder photo capture with front camera
 * - Loud alarm triggering
 *
 * Settings are persisted in [SecureStorage].
 */
@Singleton
class AntiTheftManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage
) {
    private val sensorManager: SensorManager?
        get() = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val vibrator: Vibrator?
        get() = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private var movementListener: SensorEventListener? = null
    private var chargerReceiver: BroadcastReceiver? = null
    private var isMovementAlertActive = false
    private var isChargerAlertActive = false
    private var alarmStopTime: Long = 0L

    // ── Movement Detection ─────────────────────────────────────────────

    /**
     * Enables movement-based anti-theft alert using the device accelerometer.
     * When significant movement is detected, the alarm is triggered.
     *
     * Requires [Manifest.permission.RECEIVE_BOOT_COMPLETED] for background operation.
     */
    fun enableMovementAlert(): Result<Unit> {
        val sm = sensorManager
            ?: return Result.Error(
                IllegalStateException("SensorManager not available"),
                "This device does not have the required sensors."
            )

        val accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: return Result.Error(
                IllegalStateException("Accelerometer not available"),
                "This device does not have an accelerometer."
            )

        disableMovementAlert() // Clean up any existing listener

        movementListener = object : SensorEventListener {
            private var lastX = 0f
            private var lastY = 0f
            private var lastZ = 0f
            private val movementThreshold = 2.5f // m/s^2

            override fun onSensorChanged(event: SensorEvent) {
                if (!isMovementAlertActive) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val deltaX = Math.abs(x - lastX)
                val deltaY = Math.abs(y - lastY)
                val deltaZ = Math.abs(z - lastZ)

                if (deltaX > movementThreshold || deltaY > movementThreshold || deltaZ > movementThreshold) {
                    triggerAlarm()
                }

                lastX = x
                lastY = y
                lastZ = z
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sm.registerListener(
            movementListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        isMovementAlertActive = true
        secureStorage.saveAntiTheftEnabled(true)
        return Result.Success(Unit)
    }

    /**
     * Disables the movement-based alert.
     */
    fun disableMovementAlert(): Result<Unit> {
        movementListener?.let { listener ->
            sensorManager?.unregisterListener(listener)
        }
        movementListener = null
        isMovementAlertActive = false
        secureStorage.saveAntiTheftEnabled(false)
        return Result.Success(Unit)
    }

    fun isMovementAlertEnabled(): Boolean = isMovementAlertActive

    // ── Charger Detection ──────────────────────────────────────────────

    /**
     * Enables charger disconnect alert. When the charger is unplugged,
     * the anti-theft alarm is triggered.
     */
    fun enableChargerAlert(): Result<Unit> {
        disableChargerAlert() // Clean up

        chargerReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val action = intent.action
                if (action == Intent.ACTION_POWER_DISCONNECTED && isChargerAlertActive) {
                    triggerAlarm()
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(chargerReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(chargerReceiver, filter)
        }

        isChargerAlertActive = true
        return Result.Success(Unit)
    }

    /**
     * Disables the charger disconnect alert.
     */
    fun disableChargerAlert(): Result<Unit> {
        chargerReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
        }
        chargerReceiver = null
        isChargerAlertActive = false
        return Result.Success(Unit)
    }

    fun isChargerAlertEnabled(): Boolean = isChargerAlertActive

    // ── Alarm ───────────────────────────────────────────────────────────

    /**
     * Triggers the anti-theft alarm: plays a loud sound and vibrates.
     * The alarm auto-stops after [ANTI_THEFT_ALARM_DURATION_MS] milliseconds.
     */
    fun triggerAlarm(): Result<Unit> {
        if (System.currentTimeMillis() < alarmStopTime) {
            return Result.Success(Unit) // Already alarming, cooldown active
        }

        // Play loud alarm sound
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone?.play()

            // Auto-stop after duration
            alarmStopTime = System.currentTimeMillis() + ANTI_THEFT_ALARM_DURATION_MS
            val stopIntent = Intent(context, AlarmStopReceiver::class.java)
            val stopPending = PendingIntent.getBroadcast(
                context, ANTI_THEFT_STOP_CODE, stopIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                android.os.SystemClock.elapsedRealtime() + ANTI_THEFT_ALARM_DURATION_MS,
                stopPending
            )
        } catch (_: Exception) {}

        // Vibrate
        vibrateDevice()

        // Show notification
        showAntiTheftNotification()

        return Result.Success(Unit)
    }

    /**
     * Immediately stops any active alarm.
     */
    fun stopAlarm(): Result<Unit> {
        alarmStopTime = 0L
        vibrator?.cancel()
        return Result.Success(Unit)
    }

    // ── Intruder Photo ─────────────────────────────────────────────────

    /**
     * Attempts to take a photo with the front camera to capture a potential intruder.
     * Uses an intent-based approach that works even when the screen is off.
     *
     * @return [Result] with the image [Uri] or an error message.
     */
    suspend fun takeIntruderPhoto(): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "TARZO_intruder_$timestamp.jpg"

            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_PICTURES + "/TARZO"
                    )
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val imageUri = context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext Result.Error(
                java.io.IOException("Failed to create media store entry"),
                "Could not create intruder photo."
            )

            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (cameraIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(cameraIntent)
                Result.Success(imageUri)
            } else {
                Result.Error(
                    IllegalStateException("No camera app available"),
                    "Could not access the camera for intruder photo."
                )
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to capture intruder photo: ${e.message}")
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun vibrateDevice() {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 200, 500, 200, 500),
                    intArrayOf(255, 0, 255, 0, 255, 0),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
        }
    }

    private fun showAntiTheftNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "tarzo_anti_theft"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TARZO Anti-Theft Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Anti-theft alarm notifications"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("TARZO Anti-Theft Alert")
            .setContentText("Movement or charger disconnect detected!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        notificationManager.notify(ANTI_THEFT_NOTIFICATION_ID, notification)
    }

    fun cleanup() {
        disableMovementAlert()
        disableChargerAlert()
        stopAlarm()
    }

    companion object {
        private const val ANTI_THEFT_STOP_CODE = 9001
    }
}

/**
 * Receiver that stops the anti-theft alarm after the configured duration.
 */
class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.cancel()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(com.tarzo.ai.util.Constants.ANTI_THEFT_NOTIFICATION_ID)
    }
}
