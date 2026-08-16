package com.tarzo.ai.features.translation

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tarzo.ai.core.network.ApiClient
import com.tarzo.ai.core.storage.SecureStorage
import com.tarzo.ai.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages text translation using the configured backend API or browser fallback.
 *
 * Primary strategy: If a backend API is configured (via [SecureStorage]), uses the
 * [ApiClient.translate] method to translate text server-side.
 *
 * Secondary strategy: Falls back to opening Google Translate in the browser
 * with the text and language parameters pre-filled.
 *
 * On-device ML Kit Translation could be added as a third strategy for offline use.
 */
@Singleton
class TranslationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: ApiClient,
    private val secureStorage: SecureStorage
) {

    /**
     * Translates the given text between languages.
     *
     * @param text The text to translate.
     * @param sourceLang ISO 639-1 source language code (e.g., "hi", "en").
     *                  Pass "auto" for automatic language detection.
     * @param targetLang ISO 639-1 target language code (e.g., "en", "hi").
     * @return [Result] with the translated text, or an error.
     */
    suspend fun translate(
        text: String,
        sourceLang: String = "auto",
        targetLang: String = "en"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            return@withContext Result.Error(
                IllegalArgumentException("Empty text"),
                "Text to translate cannot be empty."
            )
        }

        if (targetLang.isBlank()) {
            return@withContext Result.Error(
                IllegalArgumentException("No target language"),
                "Target language is required."
            )
        }

        // Strategy 1: Use backend API if configured
        if (apiClient.isConfigured()) {
            when (val response = apiClient.translate(text, targetLang)) {
                is Result.Success -> {
                    val translated = response.data.translatedText
                    if (translated.isNotBlank()) {
                        return@withContext Result.Success(translated)
                    }
                }
                else -> { /* Fall through to browser */ }
            }
        }

        // Strategy 2: Open Google Translate in browser
        try {
            openBrowserTranslation(text, sourceLang, targetLang)
            return@withContext Result.Success(
                "Opened Google Translate for: \"${text.take(50)}\""
            )
        } catch (e: Exception) {
            Result.Error(e, "Translation failed: ${e.message}")
        }
    }

    /**
     * Opens Google Translate in the browser with the given text pre-filled.
     * This works without any API key or configuration.
     */
    fun openBrowserTranslation(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<Unit> {
        return try {
            val sl = if (sourceLang == "auto") "auto" else normalizeLanguageCode(sourceLang)
            val tl = normalizeLanguageCode(targetLang)

            val url = "https://translate.google.com/?sl=$sl&tl=$tl&text=${
                URLEncoder.encode(text, "UTF-8")
            }&op=translate"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Could not open translator: ${e.message}")
        }
    }

    /**
     * Detects the language of the given text using a simple heuristic
     * based on Unicode range detection. Not as accurate as ML Kit or API-based
     * detection, but works offline.
     *
     * @return ISO 639-1 language code guess.
     */
    fun detectLanguage(text: String): String {
        val sample = text.take(200)
        val devanagariCount = sample.count { it in '\u0900'..'\u097F' }
        val latinCount = sample.count { it in 'A'..'Z' || it in 'a'..'z' }
        val arabicCount = sample.count { it in '\u0600'..'\u06FF' }
        val chineseCount = sample.count { it in '\u4E00'..'\u9FFF' }
        val tamilCount = sample.count { it in '\u0B80'..'\u0BFF' }
        val banglaCount = sample.count { it in '\u0980'..'\u09FF' }
        val totalLetters = sample.count { it.isLetter() }

        if (totalLetters == 0) return "en"

        val ratios = mapOf(
            "hi" to devanagariCount.toFloat() / totalLetters,
            "ar" to arabicCount.toFloat() / totalLetters,
            "zh" to chineseCount.toFloat() / totalLetters,
            "ta" to tamilCount.toFloat() / totalLetters,
            "bn" to banglaCount.toFloat() / totalLetters,
            "en" to latinCount.toFloat() / totalLetters
        )

        return ratios.maxByOrNull { it.value }?.key ?: "en"
    }

    /**
     * Returns the display name for a language code.
     */
    fun getLanguageDisplayName(code: String): String {
        return languageNames[code.lowercase()] ?: Locale(code).displayLanguage.ifBlank { code }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun normalizeLanguageCode(code: String): String {
        return when (code.lowercase()) {
            "hi", "hindi" -> "hi"
            "en", "english", "eng" -> "en"
            "ur", "urdu" -> "ur"
            "ta", "tamil" -> "ta"
            "te", "telugu" -> "te"
            "bn", "bengali", "bangla" -> "bn"
            "mr", "marathi" -> "mr"
            "gu", "gujarati" -> "gu"
            "kn", "kannada" -> "kn"
            "ml", "malayalam" -> "ml"
            "pa", "punjabi" -> "pa"
            "fr", "french", "fra" -> "fr"
            "es", "spanish", "esp" -> "es"
            "de", "german", "deu" -> "de"
            "ja", "japanese", "jpn" -> "ja"
            "zh", "chinese", "chi" -> "zh-CN"
            "ar", "arabic", "ara" -> "ar"
            "ko", "korean", "kor" -> "ko"
            "ru", "russian", "rus" -> "ru"
            "pt", "portuguese", "por" -> "pt"
            else -> code.lowercase().substring(0, 2.coerceAtMost(code.length))
        }
    }

    companion object {
        private val languageNames = mapOf(
            "hi" to "Hindi",
            "en" to "English",
            "ur" to "Urdu",
            "ta" to "Tamil",
            "te" to "Telugu",
            "bn" to "Bengali",
            "mr" to "Marathi",
            "gu" to "Gujarati",
            "kn" to "Kannada",
            "ml" to "Malayalam",
            "pa" to "Punjabi",
            "fr" to "French",
            "es" to "Spanish",
            "de" to "German",
            "ja" to "Japanese",
            "zh" to "Chinese",
            "ar" to "Arabic",
            "ko" to "Korean",
            "ru" to "Russian",
            "pt" to "Portuguese"
        )
    }
}
