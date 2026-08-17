package com.tarzo.ai

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.Room
import androidx.work.Configuration
import com.tarzo.ai.core.storage.AppDatabase
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
class TarzoApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val dataStore: DataStore<Preferences> by lazy { this.dataStore }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            DATABASE_NAME
        ).addMigrations(*AppDatabase.MIGRATIONS.toTypedArray())
            .fallbackToDestructiveMigration()
            .build()
    }

    val secureStorage: SecureStorage by lazy {
        SecureStorage(applicationContext)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeDefaults()
    }

    private fun initializeDefaults() {
        applicationScope.launch {
            val prefs = dataStore.data.first()
            val activeLanguage = prefs[KEY_ACTIVE_LANGUAGE]
            if (activeLanguage.isNullOrBlank()) {
                dataStore.edit { preferences ->
                    preferences[KEY_ACTIVE_LANGUAGE] = DEFAULT_LANGUAGE
                    preferences[KEY_WAKE_WORD_ENABLED] = "true"
                    preferences[KEY_TTS_SPEED] = DEFAULT_TTS_SPEED.toString()
                    preferences[KEY_VOICE_PITCH] = DEFAULT_VOICE_PITCH.toString()
                    preferences[KEY_ANTI_THEFT_ENABLED] = "false"
                    preferences[KEY_THEME_MODE] = "system"
                }
            }
        }
    }

    companion object {
        private const val DATABASE_NAME = "tarzo_database"
        const val DEFAULT_LANGUAGE = "hi-IN"
        const val DEFAULT_TTS_SPEED = 1.0f
        const val DEFAULT_VOICE_PITCH = 1.0f

        val KEY_ACTIVE_LANGUAGE = stringPreferencesKey("active_language")
        val KEY_WAKE_WORD_ENABLED = stringPreferencesKey("wake_word_enabled")
        val KEY_TTS_SPEED = stringPreferencesKey("tts_speed")
        val KEY_VOICE_PITCH = stringPreferencesKey("voice_pitch")
        val KEY_ANTI_THEFT_ENABLED = stringPreferencesKey("anti_theft_enabled")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

        lateinit var instance: TarzoApp
            private set
    }
}

private suspend fun DataStore<Preferences>.edit(transform: suspend (MutablePreferences) -> Unit) {
    androidx.datastore.preferences.core.edit(transform)
}
