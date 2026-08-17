package com.tarzo.ai.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure key-value storage using AndroidX Security Crypto (EncryptedSharedPreferences).
 * Data is encrypted with AES-256-GCM on device. Suitable for storing
 * sensitive config like API endpoints, user preferences, and tokens.
 */
@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return prefs.getString(key, defaultValue)
    }

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return prefs.getFloat(key, defaultValue)
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun getAll(): Map<String, *> {
        return prefs.all
    }

    // ── Typed helpers for TARZO config ────────────────────────

    fun saveApiBaseUrl(url: String) {
        putString(KEY_API_BASE_URL, url.trimEnd('/'))
    }

    fun getApiBaseUrl(): String {
        return getString(KEY_API_BASE_URL, DEFAULT_API_BASE_URL) ?: DEFAULT_API_BASE_URL
    }

    fun saveAntiTheftPin(pin: String) {
        putString(KEY_ANTI_THEFT_PIN, pin)
    }

    fun getAntiTheftPin(): String? {
        return getString(KEY_ANTI_THEFT_PIN)
    }

    fun saveAntiTheftEnabled(enabled: Boolean) {
        putBoolean(KEY_ANTI_THEFT_ENABLED, enabled)
    }

    fun isAntiTheftEnabled(): Boolean {
        return getBoolean(KEY_ANTI_THEFT_ENABLED, false)
    }

    fun saveUserPreferredLanguage(lang: String) {
        putString(KEY_PREFERRED_LANGUAGE, lang)
    }

    fun getUserPreferredLanguage(): String {
        return getString(KEY_PREFERRED_LANGUAGE, "hi-IN") ?: "hi-IN"
    }

    fun saveCustomEndpoint(path: String, url: String) {
        putString(KEY_CUSTOM_ENDPOINT_PREFIX + path, url)
    }

    fun getCustomEndpoint(path: String): String? {
        return getString(KEY_CUSTOM_ENDPOINT_PREFIX + path)
    }

    companion object {
        private const val SECURE_PREFS_NAME = "tarzo_secure_prefs"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_ANTI_THEFT_PIN = "anti_theft_pin"
        private const val KEY_ANTI_THEFT_ENABLED = "anti_theft_enabled"
        private const val KEY_PREFERRED_LANGUAGE = "preferred_language"
        private const val KEY_CUSTOM_ENDPOINT_PREFIX = "custom_endpoint_"
        private const val DEFAULT_API_BASE_URL = ""
    }
}