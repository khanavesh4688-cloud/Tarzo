package com.tarzo.ai.features.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarzo.ai.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommunicationUiState(
    val isOperationInProgress: Boolean = false,
    val lastMessage: String? = null,
    val pendingSmsRecipient: String? = null,
    val pendingSmsBody: String? = null,
    val error: String? = null
)

/**
 * Hilt ViewModel that coordinates call and SMS operations.
 * Exposes state flows for the UI to observe and provides methods
 * for dialing contacts/numbers and sending SMS.
 */
@HiltViewModel
class CommunicationViewModel @Inject constructor(
    private val callManager: CallManager,
    private val smsManager: SmsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunicationUiState())
    val uiState: StateFlow<CommunicationUiState> = _uiState.asStateFlow()

    private val _lastCallResult = MutableStateFlow<Result<String>>(Result.Success("Idle"))
    val lastCallResult: StateFlow<Result<String>> = _lastCallResult.asStateFlow()

    private val _lastSmsResult = MutableStateFlow<Result<String>>(Result.Success("Idle"))
    val lastSmsResult: StateFlow<Result<String>> = _lastSmsResult.asStateFlow()

    /**
     * Dials a contact by name.
     */
    fun dialContact(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true, error = null)
            val result = callManager.dialContact(name)
            _lastCallResult.value = result
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        lastMessage = result.data,
                        error = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Dials a phone number directly.
     */
    fun dialNumber(number: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true, error = null)
            val result = callManager.dialNumber(number)
            _lastCallResult.value = result
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        lastMessage = result.data
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Prepares an SMS to a contact. Must be confirmed via [confirmSms].
     */
    fun prepareSmsToContact(name: String, message: String) {
        val result = smsManager.prepareSmsToContact(name, message)
        when (result) {
            is Result.Success -> {
                val pending = smsManager.pendingSms.value
                _uiState.value = _uiState.value.copy(
                    pendingSmsRecipient = name,
                    pendingSmsBody = message,
                    error = null,
                    lastMessage = result.data
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(
                    error = result.message
                )
            }
            is Result.Loading -> {}
        }
    }

    /**
     * Prepares an SMS to a phone number. Must be confirmed via [confirmSms].
     */
    fun prepareSms(number: String, message: String) {
        val result = smsManager.prepareSms(number, message)
        when (result) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    pendingSmsRecipient = number,
                    pendingSmsBody = message,
                    error = null,
                    lastMessage = result.data
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(error = result.message)
            }
            is Result.Loading -> {}
        }
    }

    /**
     * Confirms and sends the pending SMS message.
     * This should only be called after the user explicitly confirms.
     */
    fun confirmSms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true)
            val result = smsManager.confirmAndSend()
            _lastSmsResult.value = result
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        pendingSmsRecipient = null,
                        pendingSmsBody = null,
                        lastMessage = result.data,
                        error = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isOperationInProgress = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Cancels the pending SMS without sending.
     */
    fun cancelSms() {
        smsManager.cancelPending()
        _uiState.value = _uiState.value.copy(
            pendingSmsRecipient = null,
            pendingSmsBody = null,
            lastMessage = "SMS cancelled."
        )
    }

    /**
     * Checks if there is a pending SMS awaiting confirmation.
     */
    fun hasPendingSms(): Boolean {
        return smsManager.pendingSms.value != null
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}