package com.tarzo.ai.features.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarzo.ai.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ReminderUiState(
    val reminders: List<ReminderItem> = emptyList(),
    val lastMessage: String? = null,
    val error: String? = null,
    val isOperationInProgress: Boolean = false
)

/**
 * Hilt ViewModel for managing alarms, timers, and reminders.
 * Exposes a [StateFlow] of [ReminderItem] list that auto-updates
 * when the underlying Room database changes.
 */
@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderManager: ReminderManager
) : ViewModel() {

    /** All reminders, sorted by time, as a live StateFlow. */
    val reminders: StateFlow<List<ReminderItem>> = reminderManager.listReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    private val _lastResult = MutableStateFlow<Result<String>>(Result.Success("Idle"))
    val lastResult: StateFlow<Result<String>> = _lastResult.asStateFlow()

    /**
     * Creates a new alarm at the specified time.
     *
     * @param timeMillis Alarm time in epoch milliseconds.
     * @param label Descriptive label for the alarm.
     */
    fun createAlarm(timeMillis: Long, label: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true, error = null)
            when (val result = reminderManager.createAlarm(timeMillis, label)) {
                is Result.Success -> {
                    val formatted = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date(timeMillis))
                    val msg = "Alarm set for $formatted"
                    _uiState.value = _uiState.value.copy(isOperationInProgress = false, lastMessage = msg)
                    _lastResult.value = Result.Success(msg)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isOperationInProgress = false, error = result.message)
                    _lastResult.value = result
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Creates a timer that fires after the given duration.
     *
     * @param durationMinutes Duration in minutes.
     * @param label Descriptive label.
     */
    fun createTimer(durationMinutes: Int, label: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true, error = null)
            when (val result = reminderManager.createTimer(durationMinutes, label)) {
                is Result.Success -> {
                    val msg = if (durationMinutes < 60) {
                        "Timer set for $durationMinutes minutes"
                    } else {
                        val hrs = durationMinutes / 60
                        val mins = durationMinutes % 60
                        "Timer set for $hrs hour${if (hrs > 1) "s" else ""} $mins minute${if (mins != 1) "s" else ""}"
                    }
                    _uiState.value = _uiState.value.copy(isOperationInProgress = false, lastMessage = msg)
                    _lastResult.value = Result.Success(msg)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isOperationInProgress = false, error = result.message)
                    _lastResult.value = result
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Creates a reminder at the specified time.
     */
    fun createReminder(timeMillis: Long, label: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true, error = null)
            when (val result = reminderManager.createReminder(timeMillis, label)) {
                is Result.Success -> {
                    val formatted = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date(timeMillis))
                    val msg = "Reminder set for $formatted: $label"
                    _uiState.value = _uiState.value.copy(isOperationInProgress = false, lastMessage = msg)
                    _lastResult.value = Result.Success(msg)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isOperationInProgress = false, error = result.message)
                    _lastResult.value = result
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Deletes a reminder by ID.
     */
    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true, error = null)
            when (val result = reminderManager.deleteReminder(id)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isOperationInProgress = false, lastMessage = "Reminder deleted")
                    _lastResult.value = Result.Success("Reminder deleted")
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isOperationInProgress = false, error = result.message)
                    _lastResult.value = result
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Cancels the alarm for a reminder without deleting it.
     */
    fun cancelAlarm(id: Long) {
        viewModelScope.launch {
            when (val result = reminderManager.cancelAlarm(id)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(lastMessage = "Alarm cancelled")
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Clears the last message and error.
     */
    fun clearState() {
        _uiState.value = _uiState.value.copy(lastMessage = null, error = null)
    }
}