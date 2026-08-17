package com.tarzo.ai.features.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarzo.ai.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaUiState(
    val isOperationInProgress: Boolean = false,
    val lastMessage: String? = null,
    val error: String? = null,
    val currentTrackTitle: String? = null
)

/**
 * Hilt ViewModel for media playback operations.
 * Controls play/pause, skip, music search, and YouTube.
 */
@HiltViewModel
class MediaViewModel @Inject constructor(
    private val mediaControlManager: MediaControlManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _lastResult = MutableStateFlow<Result<String>>(Result.Success("Idle"))
    val lastResult: StateFlow<Result<String>> = _lastResult.asStateFlow()

    /**
     * Plays music matching the given query by launching a music app.
     */
    fun playMusic(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true, error = null)
            val result = mediaControlManager.playMusic(query)
            _lastResult.value = result
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
     * Toggles play/pause on the active media session.
     */
    fun playPause() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOperationInProgress = true, error = null)
            val result = mediaControlManager.playPause()
            _lastResult.value = result
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
     * Pauses the current media playback.
     */
    fun pauseMusic() {
        viewModelScope.launch {
            val result = mediaControlManager.pauseMusic()
            _lastResult.value = result
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(lastMessage = result.data)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Skips to the next track.
     */
    fun skipTrack() {
        viewModelScope.launch {
            val result = mediaControlManager.skipTrack()
            _lastResult.value = result
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(lastMessage = result.data)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Goes to the previous track.
     */
    fun previousTrack() {
        viewModelScope.launch {
            val result = mediaControlManager.previousTrack()
            _lastResult.value = result
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(lastMessage = result.data)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Opens a YouTube search for the given query.
     */
    fun searchYouTube(query: String) {
        viewModelScope.launch {
            val result = mediaControlManager.openYouTubeSearch(query)
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(lastMessage = "Searching YouTube for: $query")
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Toggles fullscreen mode.
     */
    fun toggleFullscreen() {
        viewModelScope.launch {
            val result = mediaControlManager.toggleFullscreen()
            _lastResult.value = result
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(lastMessage = result.data)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Clears the current error.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}