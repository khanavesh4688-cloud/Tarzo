@file:Suppress("DEPRECATION")

package com.tarzo.ai.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tarzo.ai.MainActivity
import com.tarzo.ai.TarzoApp
import com.tarzo.ai.core.ai.IntentDetector
import com.tarzo.ai.core.ai.IntentResult
import com.tarzo.ai.core.ai.LLMClient
import com.tarzo.ai.core.storage.MemoryManager
import com.tarzo.ai.core.voice.SpeechRecognitionManager
import com.tarzo.ai.core.voice.SpeechState
import com.tarzo.ai.core.voice.TTSManager
import com.tarzo.ai.core.voice.WakeWordEngine
import com.tarzo.ai.core.voice.WakeWordEvent
import com.tarzo.ai.features.communication.CallManager
import com.tarzo.ai.features.communication.SmsManager
import com.tarzo.ai.features.device.DeviceControlManager
import com.tarzo.ai.features.reminders.ReminderManager
import com.tarzo.ai.features.search.SearchManager
import com.tarzo.ai.util.Constants
import com.tarzo.ai.util.Constants.IntentType
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * States the voice assistant service can be in.
 */
enum class AssistantState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

/**
 * Holds the current conversation turn for UI display.
 */
data class ConversationTurn(
    val userText: String,
    val responseText: String,
    val intentType: IntentType,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Foreground service that orchestrates the full voice assistant pipeline.
 *
 * Pipeline: WakeWordEngine -> SpeechRecognition -> IntentDetector -> LLMClient -> TTSManager -> Device Action
 *
 * This is the HEART of the TARZO app. It manages the lifecycle of all voice-related
 * components, maintains a conversation history, and provides a persistent notification
 * showing the current assistant state.
 *
 * The service can be bound to by [MainActivity] to observe state and conversation.
 */
@AndroidEntryPoint
class VoiceAssistantService : Service() {

    @Inject lateinit var wakeWordEngine: WakeWordEngine
    @Inject lateinit var speechRecognitionManager: SpeechRecognitionManager
    @Inject lateinit var ttsManager: TTSManager
    @Inject lateinit var intentDetector: IntentDetector
    @Inject lateinit var llmClient: LLMClient
    @Inject lateinit var memoryManager: MemoryManager
    @Inject lateinit var deviceControlManager: DeviceControlManager
    @Inject lateinit var callManager: CallManager
    @Inject lateinit var smsManager: SmsManager
    @Inject lateinit var reminderManager: ReminderManager
    @Inject lateinit var commandProcessor: CommandProcessor

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeWordJob: Job? = null
    private var speechJob: Job? = null

    private val _state = MutableStateFlow(AssistantState.IDLE)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _conversation = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val conversation: StateFlow<List<ConversationTurn>> = _conversation.asStateFlow()

    private val _partialSpeech = MutableStateFlow("")
    val partialSpeech: StateFlow<String> = _partialSpeech.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var currentLanguage: String = Constants.DEFAULT_LANGUAGE
    private var wakeWordEnabled: Boolean = true
    private var isBound = false

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): VoiceAssistantService = this@VoiceAssistantService
    }

    override fun onBind(intent: Intent?): IBinder {
        isBound = true
        return binder
    }

    override fun onRebind(intent: Intent?) {
        isBound = true
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isBound = false
        return true
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification("TARZO Ready"))
        loadSettings()
        ttsManager.initializeAsync()
        Log.d(TAG, "VoiceAssistantService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> startListening()
            ACTION_STOP_LISTENING -> stopListening()
            ACTION_PROCESS_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT)
                if (!text.isNullOrBlank()) processUserInput(text)
            }
            ACTION_STOP_SERVICE -> stopSelf()
            ACTION_WAKE_WORD_DETECTED -> {
                Log.d(TAG, "Wake word detected, starting speech recognition")
                startSpeechRecognition()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        wakeWordJob?.cancel()
        speechJob?.cancel()
        wakeWordEngine.stop()
        speechRecognitionManager.stopListening()
        ttsManager.stop()
        super.onDestroy()
        Log.d(TAG, "VoiceAssistantService destroyed")
    }

    // ── Public API ───────────────────────────────────────────────

    /**
     * Starts the full listening pipeline: speech recognition + optional wake word.
     */
    fun startListening() {
        serviceScope.launch {
            ttsManager.stop()
            _state.value = AssistantState.LISTENING
            _lastError.value = null
            updateNotification("Listening...")
            startSpeechRecognition()
            if (wakeWordEnabled) {
                startWakeWordDetection()
            }
        }
    }

    /**
     * Stops all listening and returns to idle.
     */
    fun stopListening() {
        wakeWordJob?.cancel()
        speechJob?.cancel()
        wakeWordEngine.stop()
        speechRecognitionManager.stopListening()
        _state.value = AssistantState.IDLE
        _partialSpeech.value = ""
        updateNotification("TARZO Ready")
    }

    /**
     * Processes a text input directly (from UI or other sources).
     */
    fun processUserInput(text: String) {
        serviceScope.launch {
            val response = handleUserCommand(text)
            _state.value = AssistantState.SPEAKING
            updateNotification("Speaking...")
            ttsManager.speakAndWait(response)
            _state.value = AssistantState.IDLE
            updateNotification("TARZO Ready")
        }
    }

    fun clearConversation() {
        _conversation.value = emptyList()
    }

    // ── Internal Pipeline ─────────────────────────────────────────

    private fun startSpeechRecognition() {
        speechJob?.cancel()
        _partialSpeech.value = ""
        speechJob = serviceScope.launch {
            speechRecognitionManager.startListening(currentLanguage)
            speechRecognitionManager.state.collect { speechState ->
                when (speechState) {
                    is SpeechState.Listening -> {
                        _state.value = AssistantState.LISTENING
                        updateNotification("Listening...")
                    }
                    is SpeechState.PartialResult -> {
                        _partialSpeech.value = speechState.text
                    }
                    is SpeechState.FinalResult -> {
                        _partialSpeech.value = ""
                        serviceScope.launch {
                            val response = handleUserCommand(speechState.text)
                            _state.value = AssistantState.SPEAKING
                            updateNotification("Speaking...")
                            ttsManager.speakAndWait(response)
                            _state.value = AssistantState.IDLE
                            updateNotification("TARZO Ready")
                        }
                    }
                    is SpeechState.Error -> {
                        Log.e(TAG, "Speech error: ${speechState.message}")
                        _lastError.value = speechState.message
                        if (speechState.code != SpeechRecognitionManager.ERROR_NO_RESULTS) {
                            _state.value = AssistantState.ERROR
                            ttsManager.speak("Mujhe samajh nahi aaya. Dobara boliye.")
                            delay(2000)
                        }
                        _state.value = AssistantState.IDLE
                        updateNotification("TARZO Ready")
                        speechJob?.cancel()
                    }
                    is SpeechState.Idle -> { /* no-op */ }
                }
            }
        }
    }

    private fun startWakeWordDetection() {
        wakeWordJob?.cancel()
        wakeWordJob = serviceScope.launch {
            wakeWordEngine.start()
            wakeWordEngine.events.collect { event ->
                when (event) {
                    is WakeWordEvent.Detected -> {
                        Log.i(TAG, "Wake word detected!")
                        ttsManager.stop()
                        _state.value = AssistantState.LISTENING
                        updateNotification("Bolo TARZO detected!")
                        startSpeechRecognition()
                    }
                    is WakeWordEvent.Error -> {
                        Log.e(TAG, "Wake word error: ${event.message}")
                    }
                    is WakeWordEvent.Listening -> {
                        Log.d(TAG, "Wake word engine listening")
                    }
                    is WakeWordEvent.AudioLevel -> {
                        // Audio level events for visualizations
                    }
                    is WakeWordEvent.Stopped -> {
                        Log.d(TAG, "Wake word engine stopped")
                    }
                }
            }
        }
    }

    private suspend fun handleUserCommand(userText: String): String {
        _state.value = AssistantState.PROCESSING
        updateNotification("Processing...")

        // Step 1: Detect intent (fast, rule-based)
        val intentResult = intentDetector.detectIntent(userText)
        Log.d(TAG, "Detected intent: ${intentResult.intent} (confidence: ${intentResult.confidence})")

        // Step 2: Execute the device action FIRST (flashlight, call, sms, etc.)
        // This runs in parallel with response generation
        val actionJob = serviceScope.launch { commandProcessor.execute(intentResult, userText) }

        // Step 3: Generate response - try LLM with streaming (fast first token), fallback to offline
        val responseText = withTimeoutOrNull(12000L) {
            llmClient.generateResponseAsync(userText, intentResult, currentLanguage)
        } ?: run {
            Log.w(TAG, "LLM timed out, using offline response")
            llmClient.generateResponse(userText, intentResult, currentLanguage)
        }

        actionJob.join() // Wait for device action to complete

        // Save to conversation history
        val turn = ConversationTurn(
            userText = userText,
            responseText = responseText,
            intentType = intentResult.intent
        )
        _conversation.value = _conversation.value + turn

        return responseText
    }

    // ── Settings ──────────────────────────────────────────────────

    private fun loadSettings() {
        serviceScope.launch {
            try {
                val prefs = TarzoApp.instance.dataStore.data.first()
                currentLanguage = prefs[TarzoApp.KEY_ACTIVE_LANGUAGE] ?: Constants.DEFAULT_LANGUAGE
                wakeWordEnabled = prefs[TarzoApp.KEY_WAKE_WORD_ENABLED]?.toBoolean() ?: true
                val speed = prefs[TarzoApp.KEY_TTS_SPEED]
                        ?.toFloatOrNull() ?: Constants.DEFAULT_TTS_SPEED
                val pitch = prefs[TarzoApp.KEY_VOICE_PITCH]
                        ?.toFloatOrNull() ?: Constants.DEFAULT_VOICE_PITCH
                ttsManager.setSpeechRate(speed)
                ttsManager.setPitch(pitch)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load settings", e)
            }
        }
    }

    // ── Notification ──────────────────────────────────────────────

    private fun buildNotification(stateText: String): Notification {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VoiceAssistantService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("TARZO Voice Assistant")
            .setContentText(stateText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(stateText: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(FOREGROUND_NOTIFICATION_ID, buildNotification(stateText))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "TARZO voice assistant ongoing notification"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TAG = "VoiceAssistantService"
        const val FOREGROUND_NOTIFICATION_ID = Constants.FOREGROUND_NOTIFICATION_ID
        const val NOTIFICATION_CHANNEL_ID = Constants.NOTIFICATION_CHANNEL_ID

        const val ACTION_START_LISTENING = "com.tarzo.ai.action.START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.tarzo.ai.action.STOP_LISTENING"
        const val ACTION_PROCESS_TEXT = "com.tarzo.ai.action.PROCESS_TEXT"
        const val ACTION_STOP_SERVICE = "com.tarzo.ai.action.STOP_SERVICE"
        const val ACTION_WAKE_WORD_DETECTED = "com.tarzo.ai.action.WAKE_WORD_DETECTED"
        const val EXTRA_TEXT = "extra_text"

        fun startIntent(context: Context): Intent {
            return Intent(context, VoiceAssistantService::class.java)
        }

        fun listenIntent(context: Context): Intent {
            return Intent(context, VoiceAssistantService::class.java).apply {
                action = ACTION_START_LISTENING
            }
        }

        fun processTextIntent(context: Context, text: String): Intent {
            return Intent(context, VoiceAssistantService::class.java).apply {
                action = ACTION_PROCESS_TEXT
                putExtra(EXTRA_TEXT, text)
            }
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, VoiceAssistantService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
        }
    }
}

/**
 * Routes detected intents to the appropriate feature manager.
 * Injected by Hilt and used by [VoiceAssistantService].
 */
class CommandProcessor @javax.inject.Inject constructor(
    private val deviceControlManager: DeviceControlManager,
    private val callManager: CallManager,
    private val smsManager: SmsManager,
    private val reminderManager: ReminderManager,
    private val memoryManager: MemoryManager,
    private val searchManager: SearchManager,
    private val screenAutomationManager: com.tarzo.ai.features.automation.ScreenAutomationManager,
    @ApplicationContext private val appContext: Context
) {

    suspend fun execute(intentResult: IntentResult, rawQuery: String) {
        when (intentResult.intent) {
            IntentType.FLASHLIGHT_ON -> {
                deviceControlManager.setFlashlightOn()
            }
            IntentType.FLASHLIGHT_OFF -> {
                deviceControlManager.setFlashlightOff()
            }
            IntentType.BRIGHTNESS_UP -> {
                deviceControlManager.increaseBrightness()
            }
            IntentType.BRIGHTNESS_DOWN -> {
                deviceControlManager.decreaseBrightness()
            }
            IntentType.VOLUME_UP -> {
                deviceControlManager.volumeUp()
            }
            IntentType.VOLUME_DOWN -> {
                deviceControlManager.volumeDown()
            }
            IntentType.WIFI_TOGGLE -> {
                deviceControlManager.toggleWifi()
            }
            IntentType.BLUETOOTH_TOGGLE -> {
                deviceControlManager.toggleBluetooth()
            }
            IntentType.OPEN_APP -> {
                val appName = intentResult.appName
                if (appName != null) {
                    launchAppByName(appName)
                }
            }
            IntentType.CALL_CONTACT -> {
                val contact = intentResult.contactName
                val phone = intentResult.phoneNumber
                when {
                    contact != null -> callManager.dialContact(contact)
                    phone != null -> callManager.dialNumber(phone)
                }
            }
            IntentType.SEND_SMS -> {
                val contact = intentResult.contactName
                val message = intentResult.messageText
                if (contact != null && !message.isNullOrBlank()) {
                    smsManager.prepareSmsToContact(contact, message)
                }
            }
            IntentType.OPEN_WHATSAPP -> {
                launchAppByName("WhatsApp")
            }
            IntentType.YOUTUBE_SEARCH -> {
                val query = intentResult.queryText
                if (!query.isNullOrBlank()) {
                    val uri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    appContext.startActivity(intent)
                }
            }
            IntentType.WEB_SEARCH -> {
                val query = intentResult.queryText
                if (!query.isNullOrBlank()) {
                    searchManager.webSearch(query)
                }
            }
            IntentType.PLAY_MUSIC -> {
                launchAppByName("Spotify")
            }
            IntentType.SET_ALARM -> {
                val time = intentResult.timeValue
                if (time != null) {
                    val parts = time.split(":")
                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        if (timeInMillis <= System.currentTimeMillis()) {
                            add(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                    reminderManager.createAlarm(cal.timeInMillis, "TARZO Alarm")
                }
            }
            IntentType.SET_TIMER -> {
                val duration = parseTimerDuration(rawQuery)
                reminderManager.createTimer(duration, "TARZO Timer")
            }
            IntentType.SET_REMINDER -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.MINUTE, 5)
                }
                reminderManager.createReminder(cal.timeInMillis, rawQuery)
            }
            IntentType.REMEMBER -> {
                val content = intentResult.queryText ?: rawQuery
                if (content.isNotBlank()) {
                    memoryManager.remember(content, Constants.CATEGORY_FACT)
                }
            }
            IntentType.FORGET -> {
                val content = intentResult.queryText
                if (!content.isNullOrBlank()) {
                    memoryManager.forgetByContent(content)
                }
            }
            IntentType.DATE_TIME -> {
                // Handled purely by LLM response text
            }
            IntentType.TAKE_PHOTO,
            IntentType.TAKE_SELFIE,
            IntentType.RECORD_VIDEO -> {
                launchAppByName("Camera")
            }
            IntentType.ANTI_THEFT -> {
                AntiTheftService.start(appContext)
            }
            IntentType.SCROLL_UP -> {
                screenAutomationManager.scrollUp()
            }
            IntentType.SCROLL_DOWN -> {
                screenAutomationManager.scrollDown()
            }
            IntentType.ANALYZE_IMAGE,
            IntentType.OCR_TEXT,
            IntentType.TRANSLATE,
            IntentType.LIST_MEMORIES -> {
                // These are UI-driven features; the response text guides the user.
            }
            IntentType.WEATHER -> {
                searchManager.getWeather()
            }
            IntentType.BATTERY_INFO -> {
                deviceControlManager.getBatteryInfo()
            }
            IntentType.GO_BACK -> {
                screenAutomationManager.goBack()
            }
            IntentType.GO_HOME -> {
                screenAutomationManager.goHome()
            }
            IntentType.GO_RECENTS -> {
                screenAutomationManager.openRecents()
            }
            IntentType.TYPE_TEXT -> {
                val text = intentResult.queryText
                if (!text.isNullOrBlank()) screenAutomationManager.typeText(text)
            }
            IntentType.CLICK_ELEMENT -> {
                val text = intentResult.queryText
                if (!text.isNullOrBlank()) screenAutomationManager.clickByText(text)
            }
            IntentType.LONG_PRESS_ELEMENT -> {
                val text = intentResult.queryText
                if (!text.isNullOrBlank()) screenAutomationManager.longPress(text)
            }
            IntentType.UNKNOWN -> {
                searchManager.webSearch(rawQuery)
            }
        }
    }

    private fun launchAppByName(appName: String) {
        val pm = appContext.packageManager
        val intent = pm.getLaunchIntentForPackage(appName.lowercase().replace(" ", ""))
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            return
        }
        val appPackages = mapOf(
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "camera" to "com.android.camera",
            "gallery" to "com.android.gallery3d",
            "photos" to "com.google.android.apps.photos",
            "settings" to "com.android.settings",
            "gmail" to "com.google.android.gm",
            "spotify" to "com.spotify.music",
            "netflix" to "com.netflix.mediaclient",
            "telegram" to "org.telegram.messenger",
            "snapchat" to "com.snapchat.android",
            "linkedin" to "com.linkedin.android"
        )
        val packageName = appPackages[appName.lowercase()]
        if (packageName != null) {
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(launchIntent)
                return
            }
        }
        // Fallback: use market search
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(marketIntent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, "download $appName app")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(webIntent)
        }
    }

    private fun parseTimerDuration(query: String): Int {
        val minutePatterns = listOf(
            Regex("(d+)s*minute"),
            Regex("(d+)*min"),
            Regex("(d+)*मिनट"),
            Regex("(d+)*minat")
        )
        for (pattern in minutePatterns) {
            val match = pattern.find(query.lowercase())
            if (match != null) {
                return match.groupValues[1].toIntOrNull()?.coerceIn(1, 1440) ?: 5
            }
        }
        val secondPatterns = listOf(
            Regex("(d+)*second"),
            Regex("(d+)*sec")
        )
        for (pattern in secondPatterns) {
            val match = pattern.find(query.lowercase())
            if (match != null) {
                val secs = match.groupValues[1].toIntOrNull() ?: 0
                return (secs / 60).coerceAtLeast(1)
            }
        }
        return 5
    }
}
