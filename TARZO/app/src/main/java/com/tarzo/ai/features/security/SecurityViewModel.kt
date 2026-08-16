package com.tarzo.ai.features.security

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarzo.ai.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityUiState(
    val isMovementAlertEnabled: Boolean = false,
    val isChargerAlertEnabled: Boolean = false,
    val isAlarmTriggered: Boolean = false,
    val intruderPhotoUri: Uri? = null,
    val lastMessage: String? = null,
    val error: String? = null
)

/**
 * Hilt ViewModel for anti-theft security features.
 * Manages movement alerts, charger alerts, intruder photo capture,
 * and alarm triggering.
 */
@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val antiTheftManager: AntiTheftManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val _lastResult = MutableStateFlow<Result<String>>(Result.Success("Idle"))
    val lastResult: StateFlow<Result<String>> = _lastResult.asStateFlow()

    /**
     * Enables movement-based anti-theft detection.
     */
    fun enableMovementAlert() {
        val result = antiTheftManager.enableMovementAlert()
        _lastResult.value = result.map { "Movement alert enabled" }
        when (result) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    isMovementAlertEnabled = true,
                    lastMessage = "Movement alert activated. TARZO will alarm if the phone is moved."
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(error = result.message)
            }
            is Result.Loading -> {}
        }
    }

    /**
     * Disables movement-based alert.
     */
    fun disableMovementAlert() {
        val result = antiTheftManager.disableMovementAlert()
        _lastResult.value = result.map { "Movement alert disabled" }
        when (result) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    isMovementAlertEnabled = false,
                    lastMessage = "Movement alert deactivated."
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(error = result.message)
            }
            is Result.Loading -> {}
        }
    }

    /**
     * Enables charger disconnect alert.
     */
    fun enableChargerAlert() {
        val result = antiTheftManager.enableChargerAlert()
        _lastResult.value = result.map { "Charger alert enabled" }
        when (result) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    isChargerAlertEnabled = true,
                    lastMessage = "Charger alert activated. TARZO will alarm if the charger is unplugged."
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(error = result.message)
            }
            is Result.Loading -> {}
        }
    }

    /**
     * Disables charger disconnect alert.
     */
    fun disableChargerAlert() {
        val result = antiTheftManager.disableChargerAlert()
        _lastResult.value = result.map { "Charger alert disabled" }
        when (result) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    isChargerAlertEnabled = false,
                    lastMessage = "Charger alert deactivated."
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(error = result.message)
            }
            is Result.Loading -> {}
        }
    }

    /**
     * Manually triggers the anti-theft alarm.
     */
    fun triggerAlarm() {
        val result = antiTheftManager.triggerAlarm()
        _lastResult.value = result.map { "Alarm triggered" }
        when (result) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    isAlarmTriggered = true,
                    lastMessage = "Anti-theft alarm is sounding!"
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(error = result.message)
            }
            is Result.Loading -> {}
        }
    }

    /**
     * Stops any active alarm.
     */
    fun stopAlarm() {
        val result = antiTheftManager.stopAlarm()
        _uiState.value = _uiState.value.copy(
            isAlarmTriggered = false,
            lastMessage = "Alarm stopped."
        )
        _lastResult.value = result.map { "Alarm stopped" }
    }

    /**
     * Takes a photo with the front camera to capture a potential intruder.
     */
    fun takeIntruderPhoto() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)
            when (val result = antiTheftManager.takeIntruderPhoto()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        intruderPhotoUri = result.data,
                        lastMessage = "Intruder photo captured."
                    )
                    _lastResult.value = Result.Success("Intruder photo saved")
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                    _lastResult.value = result
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Enables full anti-theft mode (movement + charger alerts).
     */
    fun enableFullProtection() {
        enableMovementAlert()
        enableChargerAlert()
    }

    /**
     * Disables all anti-theft features.
     */
    fun disableAllProtection() {
        disableMovementAlert()
        disableChargerAlert()
        stopAlarm()
        antiTheftManager.cleanup()
    }

    /**
     * Refreshes the UI state from the manager.
     */
    fun refreshState() {
        _uiState.value = _uiState.value.copy(
            isMovementAlertEnabled = antiTheftManager.isMovementAlertEnabled(),
            isChargerAlertEnabled = antiTheftManager.isChargerAlertEnabled()
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Do not cleanup here — anti-theft should persist
    }
}
