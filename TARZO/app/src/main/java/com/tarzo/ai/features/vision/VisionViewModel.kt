package com.tarzo.ai.features.vision

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

data class VisionUiState(
    val isAnalyzing: Boolean = false,
    val visionResult: VisionResult? = null,
    val ocrText: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class VisionViewModel @Inject constructor(
    private val visionAnalyzer: VisionAnalyzer
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisionUiState())
    val uiState: StateFlow<VisionUiState> = _uiState.asStateFlow()

    private val _analysisResult = MutableStateFlow<Result<VisionResult>>(Result.Success(VisionResult("", emptyList(), emptyList(), emptyList())))
    val analysisResult: StateFlow<Result<VisionResult>> = _analysisResult.asStateFlow()

    /**
     * Performs full analysis (OCR + object detection + image labeling) on the given image.
     */
    fun analyzeImage(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, errorMessage = null)
            when (val result = visionAnalyzer.analyzeImage(imageUri)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        visionResult = result.data,
                        ocrText = result.data.detectedText.ifBlank { null },
                        errorMessage = null
                    )
                    _analysisResult.value = result
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        errorMessage = result.message ?: "Analysis failed"
                    )
                    _analysisResult.value = result
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Performs OCR text recognition only.
     */
    fun recognizeText(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, errorMessage = null)
            when (val result = visionAnalyzer.recognizeTextOnly(imageUri)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        ocrText = result.data,
                        errorMessage = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        errorMessage = result.message ?: "Text recognition failed"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Returns the summary text of the last analysis result.
     */
    fun getSummary(): String {
        return _uiState.value.visionResult?.toSummary()
            ?: _uiState.value.ocrText
            ?: _uiState.value.errorMessage
            ?: "No analysis performed yet."
    }

    /**
     * Clears the current analysis result.
     */
    fun clearResult() {
        _uiState.value = VisionUiState()
    }
}