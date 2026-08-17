package com.tarzo.ai

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.tarzo.ai.core.permissions.PermissionManager
import com.tarzo.ai.core.voice.SpeechState
import com.tarzo.ai.core.voice.TTSState
import com.tarzo.ai.services.*
import com.tarzo.ai.ui.navigation.TarzoScaffold
import com.tarzo.ai.ui.theme.TarzoTheme
import com.tarzo.ai.features.device.DeviceControlViewModel
import com.tarzo.ai.features.communication.CommunicationViewModel
import com.tarzo.ai.features.media.MediaViewModel
import com.tarzo.ai.features.vision.VisionViewModel
import com.tarzo.ai.features.reminders.ReminderViewModel
import com.tarzo.ai.features.search.SearchViewModel
import com.tarzo.ai.features.security.SecurityViewModel
import com.tarzo.ai.features.translation.TranslationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Combined state of the voice assistant orb in the UI.
 * Merges speech recognition and TTS states into a single enum
 * that the orb animation can react to.
 */
enum class TarzoOrbState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR;

    companion object {
        fun fromAssistantState(state: AssistantState): TarzoOrbState = when (state) {
            AssistantState.IDLE -> IDLE
            AssistantState.LISTENING -> LISTENING
            AssistantState.PROCESSING -> PROCESSING
            AssistantState.SPEAKING -> SPEAKING
            AssistantState.ERROR -> ERROR
        }

        fun fromSpeechAndTTS(speechState: SpeechState, ttsState: TTSState): TarzoOrbState {
            return when {
                ttsState == TTSState.SPEAKING -> SPEAKING
                ttsState == TTSState.ERROR -> ERROR
                speechState is SpeechState.Listening -> LISTENING
                speechState is SpeechState.PartialResult -> LISTENING
                speechState is SpeechState.Error -> ERROR
                speechState is SpeechState.FinalResult -> PROCESSING
                else -> IDLE
            }
        }
    }
}

/**
 * Holds all ViewModel references that need to be initialized
 * and accessible throughout the composition.
 */
data class TarzoViewModels(
    val deviceControl: DeviceControlViewModel,
    val communication: CommunicationViewModel,
    val media: MediaViewModel,
    val vision: VisionViewModel,
    val reminders: ReminderViewModel,
    val search: SearchViewModel,
    val security: SecurityViewModel,
    val translation: TranslationViewModel
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var permissionManager: PermissionManager

    private var voiceService: VoiceAssistantService? = null
    private var isServiceBound = false

    // Shared state that child composables can access via LocalContext
    private val _orbState = MutableStateFlow(TarzoOrbState.IDLE)
    val orbState: StateFlow<TarzoOrbState> = _orbState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? VoiceAssistantService.LocalBinder ?: return
            voiceService = binder.getService()
            isServiceBound = true
            Log.d(TAG, "VoiceAssistantService bound")

            // Observe service state and propagate to orb state
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                voiceService?.state?.collect { state ->
                    _orbState.value = TarzoOrbState.fromAssistantState(state)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceService = null
            isServiceBound = false
            _orbState.value = TarzoOrbState.IDLE
            Log.d(TAG, "VoiceAssistantService disconnected")
        }
    }

    // Permission launcher for all permissions at once
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.filterValues { it }.keys
        val denied = results.filterValues { !it }.keys
        if (granted.isNotEmpty()) {
            Log.i(TAG, "Permissions granted: $granted")
        }
        if (denied.isNotEmpty()) {
            Log.w(TAG, "Permissions denied: $denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash visible until permissions are checked
        var keepSplashVisible by mutableStateOf(true)
        splashScreen.setKeepOnScreenCondition { keepSplashVisible }

        // Handle back press — minimize app instead of exiting on root
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        setContent {
            TarzoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Initialize all ViewModels
                    val deviceVm: DeviceControlViewModel = hiltViewModel()
                    val commVm: CommunicationViewModel = hiltViewModel()
                    val mediaVm: MediaViewModel = hiltViewModel()
                    val visionVm: VisionViewModel = hiltViewModel()
                    val reminderVm: ReminderViewModel = hiltViewModel()
                    val searchVm: SearchViewModel = hiltViewModel()
                    val securityVm: SecurityViewModel = hiltViewModel()
                    val translationVm: TranslationViewModel = hiltViewModel()

                    TarzoScaffold(navController = navController)

                    // Dismiss splash after initial composition
                    LaunchedEffect(Unit) {
                        delay(300L)
                        keepSplashVisible = false
                    }
                }
            }
        }

        requestPermissionsIfNeeded()
        bindVoiceService()
        Log.i(TAG, "MainActivity created")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    override fun onStart() {
        super.onStart()
        bindVoiceService()
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions in case user changed them in settings
        Log.d(TAG, "Required permissions status: allGranted=${permissionManager.hasAllPermissions()}")
    }

    override fun onStop() {
        unbindVoiceService()
        super.onStop()
    }

    override fun onDestroy() {
        unbindVoiceService()
        super.onDestroy()
    }

    // ── Intent Handling ────────────────────────────────────────────

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            VoiceAssistantService.ACTION_WAKE_WORD_DETECTED -> {
                Log.d(TAG, "Wake word action received in MainActivity")
                voiceService?.startListening()
            }
            "com.tarzo.ai.action.INCOMING_CALL" -> {
                val caller = intent.getStringExtra("extra_caller_name")
                Log.d(TAG, "Incoming call notification: $caller")
            }
        }
    }

    // ── Service Binding ────────────────────────────────────────────

    private fun bindVoiceService() {
        if (isServiceBound) return
        try {
            val intent = VoiceAssistantService.startIntent(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind voice service: ${e.message}")
        }
    }

    private fun unbindVoiceService() {
        if (isServiceBound) {
            try {
                unbindService(serviceConnection)
            } catch (_: Exception) {}
            isServiceBound = false
            voiceService = null
            _orbState.value = TarzoOrbState.IDLE
        }
    }

    // ── Voice Controls ─────────────────────────────────────────────

    /**
     * Starts listening via the bound voice assistant service.
     */
    fun startListening() {
        if (isServiceBound) {
            voiceService?.startListening()
        } else {
            bindVoiceService()
            // Retry after binding
            window.decorView.postDelayed({
                voiceService?.startListening()
            }, 500)
        }
    }

    /**
     * Stops listening via the bound voice assistant service.
     */
    fun stopListening() {
        voiceService?.stopListening()
    }

    /**
     * Sends a text command to the voice assistant service.
     */
    fun processTextCommand(text: String) {
        if (isServiceBound) {
            voiceService?.processUserInput(text)
        } else {
            val intent = VoiceAssistantService.processTextIntent(this, text)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    /**
     * Gets the conversation history from the bound service.
     */
    fun getConversation(): List<ConversationTurn> {
        return voiceService?.conversation?.value ?: emptyList()
    }

    /**
     * Gets the current partial speech text from the service.
     */
    fun getPartialSpeech(): String {
        return voiceService?.partialSpeech?.value ?: ""
    }

    /**
     * Clears the conversation history.
     */
    fun clearConversation() {
        voiceService?.clearConversation()
    }

    /**
     * Returns the bound service instance for direct access.
     */
    fun getService(): VoiceAssistantService? = voiceService

    /**
     * Returns whether the voice service is currently bound.
     */
    fun isServiceConnected(): Boolean = isServiceBound

    // ── Permissions ────────────────────────────────────────────────

    private fun requestPermissionsIfNeeded() {
        val missing = permissionManager.getMissingRequiredPermissions()
        if (missing.isEmpty()) {
            Log.i(TAG, "All required permissions granted")
            return
        }

        // On first launch, request the essential permissions
        val essentialPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )

        val toRequest = essentialPermissions.filter {
            !permissionManager.hasPermission(it)
        }.toTypedArray()

        if (toRequest.isNotEmpty()) {
            Log.i(TAG, "Requesting essential permissions: ${toRequest.toList()}")
            permissionLauncher.launch(toRequest)
        }

        // Log the status of all permission groups
        val groups = permissionManager.getMissingPermissionGroups()
        for (group in groups) {
            Log.d(TAG, "Permission group '${group.name}': ${group.missingPermissions.size} missing, required=${group.required}")
        }
    }

    /**
     * Requests a specific permission group. Can be called from settings screen.
     */
    fun requestPermissionGroup(groupName: String) {
        val group = permissionManager.getPermissionGroup(groupName)
        if (group != null) {
            val toRequest = group.permissions.filter {
                !permissionManager.hasPermission(it)
            }.toTypedArray()
            if (toRequest.isNotEmpty()) {
                permissionLauncher.launch(toRequest)
            }
        }
    }

    // ── Composable Helpers ─────────────────────────────────────────

    /**
     * Composable that observes the [VoiceAssistantService] state
     * and exposes a combined [TarzoOrbState] for the UI.
     */
    @Composable
    fun rememberTarzoOrbState(): State<TarzoOrbState> {
        val context = LocalContext.current
        val activity = context as? MainActivity
        val state by activity?.orbState?.collectAsStateWithLifecycle()
            ?: mutableStateOf(TarzoOrbState.IDLE)
        return rememberUpdatedState(state)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
