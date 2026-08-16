package com.tarzo.ai.features.device

import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarzo.ai.core.ai.IntentDetector.IntentResult
import com.tarzo.ai.util.Constants.IntentType
import com.tarzo.ai.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hilt ViewModel that exposes device state via [StateFlow] and routes
 * [IntentResult] commands to the appropriate [DeviceControlManager] action.
 */
@HiltViewModel
class DeviceControlViewModel @Inject constructor(
    private val deviceControlManager: DeviceControlManager
) : ViewModel() {

    // ── Flashlight State ───────────────────────────────────────────────

    private val _flashlightState = MutableStateFlow(false)
    val flashlightState: StateFlow<Boolean> = _flashlightState.asStateFlow()

    // ── Brightness State ───────────────────────────────────────────────

    private val _brightnessLevel = MutableStateFlow(deviceControlManager.getCurrentBrightness())
    val brightnessLevel: StateFlow<Int> = _brightnessLevel.asStateFlow()

    // ── Volume State ───────────────────────────────────────────────────

    private val _volumeInfo = MutableStateFlow(deviceControlManager.getVolumeInfo())
    val volumeInfo: StateFlow<DeviceControlManager.VolumeInfo> = _volumeInfo.asStateFlow()

    // ── Wi-Fi State ────────────────────────────────────────────────────

    private val _wifiEnabled = MutableStateFlow(deviceControlManager.isWifiEnabled())
    val wifiEnabled: StateFlow<Boolean> = _wifiEnabled.asStateFlow()

    // ── Bluetooth State ────────────────────────────────────────────────

    private val _bluetoothEnabled = MutableStateFlow(deviceControlManager.isBluetoothEnabled())
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    // ── Battery State ──────────────────────────────────────────────────

    private val _batteryInfo = MutableStateFlow(deviceControlManager.getBatteryInfo())
    val batteryInfo: StateFlow<DeviceControlManager.BatteryInfo> = _batteryInfo.asStateFlow()

    // ── Command Result ─────────────────────────────────────────────────

    private val _lastCommandResult = MutableStateFlow<Result<String>>(Result.Success("Idle"))
    val lastCommandResult: StateFlow<Result<String>> = _lastCommandResult.asStateFlow()

    // ── Execute Intent ─────────────────────────────────────────────────

    /**
     * Routes an [IntentResult] to the appropriate device control action.
     * This is the single entry point called by the command dispatcher.
     */
    fun executeCommand(intentResult: IntentResult) {
        viewModelScope.launch {
            val result: Result<String> = when (intentResult.intent) {
                IntentType.FLASHLIGHT_ON -> {
                    deviceControlManager.setFlashlightOn().map {
                        _flashlightState.value = true
                        "Flashlight turned on"
                    }
                }

                IntentType.FLASHLIGHT_OFF -> {
                    deviceControlManager.setFlashlightOff().map {
                        _flashlightState.value = false
                        "Flashlight turned off"
                    }
                }

                IntentType.BRIGHTNESS_UP -> {
                    deviceControlManager.increaseBrightness().map { level ->
                        _brightnessLevel.value = level
                        "Brightness increased to ${((level * 100) / 255)}%"
                    }
                }

                IntentType.BRIGHTNESS_DOWN -> {
                    deviceControlManager.decreaseBrightness().map { level ->
                        _brightnessLevel.value = level
                        "Brightness decreased to ${((level * 100) / 255)}%"
                    }
                }

                IntentType.VOLUME_UP -> {
                    deviceControlManager.volumeUp().map { vol ->
                        _volumeInfo.value = deviceControlManager.getVolumeInfo()
                        val pct = if (_volumeInfo.value.max > 0) {
                            (vol * 100 / _volumeInfo.value.max)
                        } else { vol }
                        "Volume increased to $pct%"
                    }
                }

                IntentType.VOLUME_DOWN -> {
                    deviceControlManager.volumeDown().map { vol ->
                        _volumeInfo.value = deviceControlManager.getVolumeInfo()
                        val pct = if (_volumeInfo.value.max > 0) {
                            (vol * 100 / _volumeInfo.value.max)
                        } else { vol }
                        "Volume decreased to $pct%"
                    }
                }

                IntentType.WIFI_TOGGLE -> {
                    deviceControlManager.toggleWifi().map { enabled ->
                        _wifiEnabled.value = enabled
                        "Wi-Fi is now ${if (enabled) "enabled" else "disabled"}"
                    }
                }

                IntentType.BLUETOOTH_TOGGLE -> {
                    deviceControlManager.toggleBluetooth().map { enabled ->
                        _bluetoothEnabled.value = enabled
                        "Bluetooth is now ${if (enabled) "enabled" else "disabled"}"
                    }
                }

                IntentType.BATTERY_INFO -> {
                    val info = deviceControlManager.getBatteryInfo()
                    _batteryInfo.value = info
                    Result.Success(info.statusText)
                }

                IntentType.DATE_TIME -> {
                    val now = java.text.SimpleDateFormat(
                        "EEEE, dd MMMM yyyy, hh:mm a",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date())
                    Result.Success("It is $now")
                }

                else -> Result.Error(
                    IllegalArgumentException("Unhandled intent: ${intentResult.intent}"),
                    "This command is not handled by device control."
                )
            }
            _lastCommandResult.value = result
        }
    }

    // ── Direct Methods ─────────────────────────────────────────────────

    fun refreshBatteryInfo() {
        _batteryInfo.value = deviceControlManager.getBatteryInfo()
    }

    fun refreshVolumeInfo() {
        _volumeInfo.value = deviceControlManager.getVolumeInfo()
    }

    fun refreshAllStates() {
        _batteryInfo.value = deviceControlManager.getBatteryInfo()
        _volumeInfo.value = deviceControlManager.getVolumeInfo()
        _brightnessLevel.value = deviceControlManager.getCurrentBrightness()
        _wifiEnabled.value = deviceControlManager.isWifiEnabled()
        _bluetoothEnabled.value = deviceControlManager.isBluetoothEnabled()
    }
}
