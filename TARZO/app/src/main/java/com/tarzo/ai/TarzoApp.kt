package com.tarzo.ai

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tarzo.ai.core.storage.SecureStorage
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tarzo_settings")

@HiltAndroidApp
class TarzoApp : Application() {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val dataStore: DataStore<Preferences> by lazy { this.dataStore }

    val secureStorage: SecureStorage by lazy {
        SecureStorage(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeDefaults()
    }

    private fun initializeDefaults() {
        applicationScope.launch {
            secureStorage.seedDefaultApiKeyIfNeeded()
            val prefs = dataStore.data.first()
            if (prefs[KEY_ACTIVE_LANGUAGE].isNullOrBlank()) {
                dataStore.edit { it[KEY_ACTIVE_LANGUAGE] = DEFAULT_LANGUAGE }
            }
        }
    }

    companion object {
        const val DEFAULT_LANGUAGE = "hi-IN"
        val KEY_ACTIVE_LANGUAGE = stringPreferencesKey("active_language")
        val KEY_WAKE_WORD_ENABLED = stringPreferencesKey("wake_word_enabled")
        val KEY_TTS_SPEED = stringPreferencesKey("tts_speed")
        val KEY_VOICE_PITCH = stringPreferencesKey("voice_pitch")
        val KEY_ANTI_THEFT_ENABLED = stringPreferencesKey("anti_theft_enabled")
        lateinit var instance: TarzoApp
            private set
    }
}
