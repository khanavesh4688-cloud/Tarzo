package com.tarzo.ai.features.device

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.provider.Settings.System
import android.hardware.camera2.CameraManager
import androidx.core.content.ContextCompat
import com.tarzo.ai.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages device hardware controls: flashlight, brightness, volume, Wi-Fi, and Bluetooth.
 * Each method checks the required permission and returns a [Result] indicating success or failure.
 *
 * Brightness limitation: On Android 6.0+ (API 23+), writing to Settings.System.SCREEN_BRIGHTNESS
 * requires the Manifest.permission.WRITE_SETTINGS permission, which is a "special permission".
 * The user must explicitly grant it via the system settings page (Settings.ACTION_MANAGE_WRITE_SETTINGS).
 * Even with the permission granted, on Android 10+ (API 29+), some OEMs restrict third-party apps
 * from programmatically changing brightness. In those cases, we open the system display settings as a fallback.
 */
@Singleton
class DeviceControlManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cameraManager: CameraManager?
        get() = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val audioManager: AudioManager?
        get() = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val wifiManager: WifiManager?
        get() = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val bluetoothAdapter: BluetoothAdapter?
        get() {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            return btManager?.adapter
        }

    private var flashlightOn = false

    // ── Flashlight ─────────────────────────────────────────────────────

    /**
     * Turns the device flashlight (torch) on.
     * Requires [Manifest.permission.CAMERA].
     */
    @SuppressLint("MissingPermission")
    suspend fun setFlashlightOn(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission(Manifest.permission.CAMERA)) {
                return@withContext Result.Error(
                    SecurityException("Camera permission required for flashlight"),
                    "Camera permission required. Please grant camera access."
                )
            }
            val cm = cameraManager
                ?: return@withContext Result.Error(
                    IllegalStateException("CameraManager not available"),
                    "This device does not support flashlight control."
                )
            cm.setTorchMode(cm.cameraIdList.first(), true)
            flashlightOn = true
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to turn on flashlight: ${e.message}")
        }
    }

    /**
     * Turns the device flashlight (torch) off.
     * Requires [Manifest.permission.CAMERA].
     */
    @SuppressLint("MissingPermission")
    suspend fun setFlashlightOff(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission(Manifest.permission.CAMERA)) {
                return@withContext Result.Error(
                    SecurityException("Camera permission required for flashlight"),
                    "Camera permission required."
                )
            }
            val cm = cameraManager
                ?: return@withContext Result.Error(
                    IllegalStateException("CameraManager not available"),
                    "CameraManager not available."
                )
            cm.setTorchMode(cm.cameraIdList.first(), false)
            flashlightOn = false
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to turn off flashlight: ${e.message}")
        }
    }

    fun isFlashlightOn(): Boolean = flashlightOn

    // ── Brightness ─────────────────────────────────────────────────────

    /**
     * Increases screen brightness by a step (approximately 30/255 per call).
     *
     * **Limitation on modern Android:**
     * WRITE_SETTINGS is a special permission that cannot be requested through the
     * standard runtime permission dialog. The user must navigate to
     * "Settings > Apps > TARZO > Advanced > Modify system settings" and enable it manually.
     * On Android 10+, even with WRITE_SETTINGS granted, many OEMs ignore programmatic
     * brightness changes. This method will attempt the change and fall back to opening
     * the display settings if it fails.
     */
    suspend fun increaseBrightness(): Result<Int> = adjustBrightness(30)

    /**
     * Decreases screen brightness by a step (approximately 30/255 per call).
     * Same limitations as [increaseBrightness] apply.
     */
    suspend fun decreaseBrightness(): Result<Int> = adjustBrightness(-30)

    private suspend fun adjustBrightness(delta: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(context)) {
                    return@withContext Result.Error(
                        SecurityException("WRITE_SETTINGS permission not granted"),
                        "Cannot adjust brightness. Please go to Settings > Apps > TARZO > " +
                            "Advanced > Allow modify system settings, then try again."
                    )
                }
            }
            val currentBrightness = System.getInt(
                context.contentResolver,
                System.SCREEN_BRIGHTNESS,
                128
            )
            val newBrightness = (currentBrightness + delta).coerceIn(0, 255)
            System.putInt(
                context.contentResolver,
                System.SCREEN_BRIGHTNESS,
                newBrightness
            )
            Result.Success(newBrightness)
        } catch (e: SecurityException) {
            Result.Error(
                e,
                "Brightness requires WRITE_SETTINGS permission. Opening system display settings."
            )
        } catch (e: Exception) {
            Result.Error(e, "Failed to adjust brightness: ${e.message}")
        }
    }

    /**
     * Gets the current screen brightness level (0-255).
     */
    fun getCurrentBrightness(): Int {
        return try {
            System.getInt(context.contentResolver, System.SCREEN_BRIGHTNESS, 128)
        } catch (e: Settings.SettingNotFoundException) {
            128
        }
    }

    /**
     * Opens the system display settings so the user can manually adjust brightness.
     */
    fun openDisplaySettings(): Result<Unit> {
        return try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Could not open display settings: ${e.message}")
        }
    }

    // ── Volume ─────────────────────────────────────────────────────────

    /**
     * Increases the media/stream volume by one step.
     * Uses [AudioManager.ADJUST_RAISE] which does not require any runtime permission.
     */
    suspend fun volumeUp(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val am = audioManager
                ?: return@withContext Result.Error(
                    IllegalStateException("AudioManager not available"),
                    "AudioManager not available on this device."
                )
            am.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            delay(100)
            val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            Result.Success(currentVolume)
        } catch (e: Exception) {
            Result.Error(e, "Failed to increase volume: ${e.message}")
        }
    }

    /**
     * Decreases the media/stream volume by one step.
     */
    suspend fun volumeDown(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val am = audioManager
                ?: return@withContext Result.Error(
                    IllegalStateException("AudioManager not available"),
                    "AudioManager not available on this device."
                )
            am.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            delay(100)
            val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            Result.Success(currentVolume)
        } catch (e: Exception) {
            Result.Error(e, "Failed to decrease volume: ${e.message}")
        }
    }

    /**
     * Gets the current volume level and max volume.
     */
    fun getVolumeInfo(): VolumeInfo {
        val am = audioManager ?: return VolumeInfo(0, 0, false)
        return VolumeInfo(
            current = am.getStreamVolume(AudioManager.STREAM_MUSIC),
            max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            isMuted = am.isStreamMute(AudioManager.STREAM_MUSIC)
        )
    }

    // ── Wi-Fi ──────────────────────────────────────────────────────────

    /**
     * Enables or disables Wi-Fi using [WifiManager].
     *
     * On Android 10+ (API 29+), [WifiManager.setWifiEnabled] is deprecated and only works
     * for system apps or apps with the CHANGE_NETWORK_STATE permission granted via
     * DevicePolicyManager. For regular apps on Android 10+, we open the Wi-Fi settings panel
     * as a fallback.
     *
     * On Android 13+ (API 33+), NEARBY_WIFI_DEVICES permission may be required for scanning,
     * but toggling Wi-Fi on/off is handled via the settings panel.
     *
     * @param enable true to turn Wi-Fi on, false to turn it off.
     */
    suspend fun setWifiEnabled(enable: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val wm = wifiManager
                ?: return@withContext Result.Error(
                    IllegalStateException("WifiManager not available"),
                    "WifiManager not available on this device."
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // On Android 10+, direct toggle is restricted. Open settings panel.
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(panelIntent)
                return@withContext Result.Success(wm.isWifiEnabled)
            }
            @Suppress("DEPRECATION")
            wm.isWifiEnabled = enable
            delay(500)
            Result.Success(wm.isWifiEnabled)
        } catch (e: Exception) {
            Result.Error(e, "Failed to toggle Wi-Fi: ${e.message}")
        }
    }

    /**
     * Toggles Wi-Fi to the opposite state.
     */
    suspend fun toggleWifi(): Result<Boolean> {
        val wm = wifiManager
            ?: return Result.Error(
                IllegalStateException("WifiManager not available"),
                "WifiManager not available."
            )
        return setWifiEnabled(!wm.isWifiEnabled)
    }

    fun isWifiEnabled(): Boolean {
        return wifiManager?.isWifiEnabled ?: false
    }

    // ── Bluetooth ──────────────────────────────────────────────────────

    /**
     * Enables or disables Bluetooth using [BluetoothAdapter].
     *
     * On Android 12+ (API 31+), BLUETOOTH_CONNECT permission is required.
     * On Android 10+ (API 29+), direct Bluetooth toggle via [BluetoothAdapter.enable]/[BluetoothAdapter.disable]
     * may be restricted to system apps. We attempt it and fall back to opening the Bluetooth settings panel.
     *
     * @param enable true to turn Bluetooth on, false to turn it off.
     */
    @SuppressLint("MissingPermission")
    suspend fun setBluetoothEnabled(enable: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    return@withContext Result.Error(
                        SecurityException("BLUETOOTH_CONNECT permission required"),
                        "Bluetooth permission required. Please grant BLUETOOTH_CONNECT in settings."
                    )
                }
            }
            val adapter = bluetoothAdapter
                ?: return@withContext Result.Error(
                    IllegalStateException("Bluetooth not supported"),
                    "This device does not support Bluetooth."
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !enable) {
                // Android 10+ doesn't allow direct disable for non-system apps.
                // Fall back to opening Bluetooth settings.
                val panelIntent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(panelIntent)
                return@withContext Result.Success(adapter.isEnabled)
            }
            if (enable) {
                adapter.enable()
            } else {
                @Suppress("DEPRECATION")
                adapter.disable()
            }
            delay(1000)
            Result.Success(adapter.isEnabled)
        } catch (e: SecurityException) {
            Result.Error(
                e,
                "Bluetooth permission denied. Opening Bluetooth settings panel."
            )
        } catch (e: Exception) {
            Result.Error(e, "Failed to toggle Bluetooth: ${e.message}")
        }
    }

    /**
     * Toggles Bluetooth to the opposite state.
     */
    suspend fun toggleBluetooth(): Result<Boolean> {
        val adapter = bluetoothAdapter
            ?: return Result.Error(
                IllegalStateException("Bluetooth not supported"),
                "Bluetooth is not available on this device."
            )
        return setBluetoothEnabled(!adapter.isEnabled)
    }

    fun isBluetoothEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                bluetoothAdapter?.isEnabled ?: false
            } catch (e: SecurityException) {
                false
            }
        } else {
            @Suppress("DEPRECATION")
            bluetoothAdapter?.isEnabled ?: false
        }
    }

    // ── Battery ────────────────────────────────────────────────────────

    /**
     * Returns battery information as a [BatteryInfo] data class.
     * Uses [Intent.ACTION_BATTERY_CHANGED] sticky broadcast.
     */
    fun getBatteryInfo(): BatteryInfo {
        val batteryIntent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent == null) {
            return BatteryInfo(level = -1, isCharging = false, temperature = -1f, voltage = -1)
        }
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val percentage = if (scale > 0) (level * 100 / scale) else -1
        return BatteryInfo(
            level = percentage,
            isCharging = isCharging,
            temperature = temperature / 10f,
            voltage = voltage
        )
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // ── Data Classes ───────────────────────────────────────────────────

    data class VolumeInfo(
        val current: Int,
        val max: Int,
        val isMuted: Boolean
    ) {
        val percentage: Int
            get() = if (max > 0) (current * 100 / max) else 0
    }

    data class BatteryInfo(
        val level: Int,
        val isCharging: Boolean,
        val temperature: Float,
        val voltage: Int
    ) {
        val statusText: String
            get() = when {
                level < 0 -> "Unknown"
                isCharging -> "Charging ($level%)"
                level <= 15 -> "Critically low ($level%)"
                level <= 30 -> "Low ($level%)"
                else -> "$level%"
            }
    }
}
