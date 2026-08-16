package com.tarzo.ai.util

/**
 * All application-wide constants for TARZO.
 */
object Constants {

    // ── Intent Types ──────────────────────────────────────────────
    enum class IntentType {
        FLASHLIGHT_ON,
        FLASHLIGHT_OFF,
        BRIGHTNESS_UP,
        BRIGHTNESS_DOWN,
        VOLUME_UP,
        VOLUME_DOWN,
        OPEN_APP,
        CALL_CONTACT,
        SEND_SMS,
        OPEN_WHATSAPP,
        YOUTUBE_SEARCH,
        PLAY_MUSIC,
        WEB_SEARCH,
        WEATHER,
        BATTERY_INFO,
        DATE_TIME,
        SET_ALARM,
        SET_TIMER,
        SET_REMINDER,
        TAKE_PHOTO,
        TAKE_SELFIE,
        RECORD_VIDEO,
        SCROLL_UP,
        SCROLL_DOWN,
        ANALYZE_IMAGE,
        OCR_TEXT,
        TRANSLATE,
        REMEMBER,
        FORGET,
        LIST_MEMORIES,
        ANTI_THEFT,
        WIFI_TOGGLE,
        BLUETOOTH_TOGGLE,
        UNKNOWN
    }

    // ── Supported Languages ──────────────────────────────────────
    data class Language(val code: String, val displayName: String, val ttsLocale: String)

    val SUPPORTED_LANGUAGES = listOf(
        Language("hi-IN", "Hindi (India)", "hi-IN"),
        Language("en-IN", "English (India)", "en-IN"),
        Language("en-US", "English (US)", "en-US")
    )

    const val DEFAULT_LANGUAGE = "hi-IN"
    const val DEFAULT_TTS_SPEED = 1.0f
    const val DEFAULT_VOICE_PITCH = 1.0f
    const val DEFAULT_WAKE_WORD = "Bolo TARZO"

    // ── Animation Durations (ms) ──────────────────────────────────
    const val ANIM_MIC_PULSE_DURATION = 1200L
    const val ANIM_RIPPLE_DURATION = 800L
    const val ANIM_FADE_IN = 300L
    const val ANIM_FADE_OUT = 200L
    const val ANIM_SLIDE_UP = 350L
    const val ANIM_SLIDE_DOWN = 300L
    const val ANIM_WAVE_DURATION = 1500L

    // ── API Endpoints ─────────────────────────────────────────────
    const val API_BASE_URL = ""
    const val ENDPOINT_WEB_SEARCH = "/api/v1/search"
    const val ENDPOINT_WEATHER = "/api/v1/weather"
    const val ENDPOINT_TRANSLATE = "/api/v1/translate"
    const val ENDPOINT_AI_CHAT = "/api/v1/chat"
    const val ENDPOINT_ANTI_THEFT = "/api/v1/anti-theft"

    // ── Wake Word ─────────────────────────────────────────────────
    const val WAKE_WORD_ENERGY_THRESHOLD = 1500.0
    const val WAKE_WORD_SILENCE_TIMEOUT_MS = 500L
    const val WAKE_WORD_CANDIDATE_TIMEOUT_MS = 2500L
    const val WAKE_WORD_MIN_SIMILARITY = 0.65f

    // ── Audio ─────────────────────────────────────────────────────
    const val AUDIO_SAMPLE_RATE = 16000
    const val AUDIO_CHANNEL_CONFIG = 1
    const val AUDIO_ENCODING = 2

    // ── Database ──────────────────────────────────────────────────
    const val DATABASE_NAME = "tarzo_database"
    const val DATABASE_VERSION = 1

    // ── Secure Storage ────────────────────────────────────────────
    const val SECURE_PREFS_FILE = "tarzo_secure_prefs"

    // ── DataStore ─────────────────────────────────────────────────
    const val DATASTORE_NAME = "tarzo_settings"

    // ── Intent Detection ──────────────────────────────────────────
    const val MIN_CONFIDENCE_THRESHOLD = 0.3f
    const val HIGH_CONFIDENCE_THRESHOLD = 0.8f

    // ── Notification ──────────────────────────────────────────────
    const val NOTIFICATION_CHANNEL_ID = "tarzo_voice"
    const val NOTIFICATION_CHANNEL_NAME = "TARZO Voice Assistant"
    const val FOREGROUND_NOTIFICATION_ID = 1001
    const val ANTI_THEFT_NOTIFICATION_ID = 1002

    // ── Anti-Theft ────────────────────────────────────────────────
    const val ANTI_THEFT_WRONG_PIN_LIMIT = 3
    const val ANTI_THEFT_ALARM_DURATION_MS = 30000L
    const val ANTI_THEFT_COOLDOWN_MS = 60000L

    // ── Memory Categories ─────────────────────────────────────────
    const val CATEGORY_PREFERENCE = "PREFERENCE"
    const val CATEGORY_FACT = "FACT"
    const val CATEGORY_CONTACT = "CONTACT"
    const val CATEGORY_SETTING = "SETTING"

    // ── Regex Patterns ────────────────────────────────────────────
    val TIME_PATTERN = Regex("(\\d{1,2})[.:]?(\\d{2})?\\s*(am|pm|baje)?", RegexOption.IGNORE_CASE)
    val PHONE_PATTERN = Regex("\\d{10}")
    val DATE_PATTERN = Regex("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})")
}
