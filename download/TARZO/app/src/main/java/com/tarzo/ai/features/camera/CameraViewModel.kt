package com.tarzo.ai.features.camera

import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarzo.ai.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val lastPhotoUri: Uri? = null,
    val isRecording: Boolean = false,
    val isCameraOpen: Boolean = false,
    val isFrontCamera: Boolean = false,
    val message: String? = null,
    val operationInProgress: Boolean = false
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraManager: CameraManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _lastResult = MutableStateFlow<Result<Uri>>(Result.Success(Uri.EMPTY))
    val lastResult: StateFlow<Result<Uri>> = _lastResult.asStateFlow()

    /**
     * Opens the camera and binds to the given [PreviewView].
     */
    fun openCamera(previewView: PreviewView, useFrontCamera: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operationInProgress = true)
            when (val result = cameraManager.openCamera(previewView, useFrontCamera)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCameraOpen = true,
                        isFrontCamera = useFrontCamera,
                        message = if (useFrontCamera) "Front camera opened" else "Camera opened",
                        operationInProgress = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        message = result.message ?: "Failed to open camera",
                        operationInProgress = false
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Takes a photo with the rear camera.
     */
    fun takePhoto() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operationInProgress = true)
            val result = cameraManager.takePhoto()
            _lastResult.value = result
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        lastPhotoUri = result.data,
                        message = "Photo saved successfully",
                        operationInProgress = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        message = result.message ?: "Failed to take photo",
                        operationInProgress = false
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Takes a selfie with the front camera.
     */
    fun takeSelfie() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operationInProgress = true)
            val result = cameraManager.takeSelfie()
            _lastResult.value = result
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        lastPhotoUri = result.data,
                        message = "Selfie saved successfully",
                        operationInProgress = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        message = result.message ?: "Failed to take selfie",
                        operationInProgress = false
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Starts recording a video.
     */
    fun startVideoRecording() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operationInProgress = true)
            val result = cameraManager.startVideoRecording()
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isRecording = true,
                        message = "Video recording started",
                        operationInProgress = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        message = result.message ?: "Failed to start recording",
                        operationInProgress = false
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Stops the current video recording.
     */
    fun stopVideoRecording() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operationInProgress = true)
            val result = cameraManager.stopVideoRecording()
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isRecording = false,
                        message = "Video recording stopped and saved",
                        operationInProgress = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isRecording = false,
                        message = result.message ?: "Failed to stop recording",
                        operationInProgress = false
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Captures a photo after a countdown timer.
     * @param seconds Number of seconds to wait before capturing.
     */
    fun captureWithTimer(seconds: Int, previewView: PreviewView? = null, useFrontCamera: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operationInProgress = true, message = "Capturing in $seconds seconds...")
            val result = cameraManager.captureWithTimer(seconds, previewView, useFrontCamera)
            _lastResult.value = result
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        lastPhotoUri = result.data,
                        message = "Timer photo captured successfully",
                        operationInProgress = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        message = result.message ?: "Timer capture failed",
                        operationInProgress = false
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Switches between front and rear camera.
     */
    fun flipCamera() {
        val newState = !_uiState.value.isFrontCamera
        _uiState.value = _uiState.value.copy(isFrontCamera = newState)
    }

    /**
     * Clears the current message.
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.cleanup()
    }
}
