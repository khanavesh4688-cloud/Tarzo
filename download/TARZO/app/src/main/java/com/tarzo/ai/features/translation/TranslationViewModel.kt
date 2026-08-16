package com.tarzo.ai.features.translation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarzo.ai.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TranslationUiState(
    val isTranslating: Boolean = false,
    val sourceText: String = "",
    val translatedText: String = "",
    val sourceLanguage: String = "auto",
    val targetLanguage: String = "en",
    val detectedLanguage: String? = null,
    val error: String? = null
)

/**
 * Hilt ViewModel for text translation operations.
 * Exposes translation state and provides methods to translate,
 * detect language, and swap languages.
 */
@HiltViewModel
class TranslationViewModel @Inject constructor(
    private val translationManager: TranslationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslationUiState())
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    private val _lastResult = MutableStateFlow<Result<String>>(Result.Success("Idle"))
    val lastResult: StateFlow<Result<String>> = _lastResult.asStateFlow()

    /**
     * Translates the given text from source to target language.
     * Auto-detects the source language if sourceLang is "auto".
     */
    fun translate(text: String, sourceLang: String = "auto", targetLang: String = "en") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTranslating = true,
                sourceText = text,
                error = null
            )

            val effectiveSource = if (sourceLang == "auto") {
                val detected = translationManager.detectLanguage(text)
                _uiState.value = _uiState.value.copy(detectedLanguage = detected)
                detected
            } else {
                sourceLang
            }

            _uiState.value = _uiState.value.copy(
                sourceLanguage = effectiveSource,
                targetLanguage = targetLang
            )

            when (val result = translationManager.translate(text, effectiveSource, targetLang)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isTranslating = false,
                        translatedText = result.data
                    )
                    _lastResult.value = result
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isTranslating = false,
                        error = result.message
                    )
                    _lastResult.value = result
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Detects the language of the given text and updates the UI state.
     */
    fun detectLanguage(text: String) {
        val detected = translationManager.detectLanguage(text)
        val displayName = translationManager.getLanguageDisplayName(detected)
        _uiState.value = _uiState.value.copy(
            detectedLanguage = detected,
            sourceLanguage = detected
        )
        _lastResult.value = Result.Success("Detected language: $displayName ($detected)")
    }

    /**
     * Swaps source and target languages.
     */
    fun swapLanguages() {
        val state = _uiState.value
        _uiState.value = state.copy(
            sourceLanguage = state.targetLanguage,
            targetLanguage = state.sourceLanguage,
            sourceText = state.translatedText,
            translatedText = state.sourceText
        )
    }

    /**
     * Gets the display name for a language code.
     */
    fun getLanguageDisplayName(code: String): String {
        return translationManager.getLanguageDisplayName(code)
    }

    /**
     * Clears the translation state.
     */
    fun clearState() {
        _uiState.value = TranslationUiState()
    }
}