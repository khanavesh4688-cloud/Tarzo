package com.tarzo.ai.features.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityManager
import com.tarzo.ai.services.TarzoAccessibilityService
import com.tarzo.ai.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides screen automation actions via an AccessibilityService connection.
 *
 * **Important Limitation:**
 * All operations in this class require that [TarzoAccessibilityService] is enabled
 * in the device's Accessibility settings (Settings > Accessibility > TARZO).
 * If the service is not enabled, every method returns a [Result.Error] with a
 * clear message directing the user to enable it.
 *
 * Communication between this manager and the AccessibilityService is done via
 * broadcast intents with action "com.tarzo.ai.ACTION_SCREEN_AUTOMATION". The service
 * listens for these actions and performs the actual accessibility operations.
 */
@Singleton
class ScreenAutomationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val ACTION_SCREEN_AUTOMATION = "com.tarzo.ai.ACTION_SCREEN_AUTOMATION"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_PARAM = "param"
        const val EXTRA_REQUEST_ID = "request_id"
        const val ACTION_RESULT = "com.tarzo.ai.ACTION_SCREEN_RESULT"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_SUCCESS = "success"

        const val COMMAND_SCROLL_UP = "scroll_up"
        const val COMMAND_SCROLL_DOWN = "scroll_down"
        const val COMMAND_SCROLL_TO_TOP = "scroll_to_top"
        const val COMMAND_SCROLL_TO_BOTTOM = "scroll_to_bottom"
        const val COMMAND_CLICK_BY_DESC = "click_by_description"
        const val COMMAND_CLICK_BY_TEXT = "click_by_text"
        const val COMMAND_GET_SCREEN_TEXT = "get_screen_text"
        const val COMMAND_PERFORM_ACTION = "perform_action"

        private var requestIdCounter = 0L
        private fun nextRequestId(): Long = ++requestIdCounter
    }

    /**
     * Checks if TARZO's AccessibilityService is enabled in system settings.
     */
    fun isAccessibilityEnabled(): Boolean {
        var enabled = false
        try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return false
            val service = ComponentName(context, TarzoAccessibilityService::class.java)
            enabled = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            ).any { it.resolveInfo.serviceInfo.packageName == context.packageName }
        } catch (_: Exception) {
            // Fall back to checking settings
            try {
                val settingValue = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
                enabled = settingValue?.contains(context.packageName) == true
            } catch (_: Exception) {}
        }
        return enabled
    }

    /**
     * Opens the Accessibility settings page so the user can enable TARZO's service.
     */
    fun openAccessibilitySettings(): Result<Unit> {
        return try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Could not open accessibility settings: ${e.message}")
        }
    }

    /**
     * Scrolls the current screen content to the top.
     * Requires accessibility service to be enabled.
     */
    suspend fun scrollToTop(): Result<String> = sendCommand(COMMAND_SCROLL_TO_TOP)

    /**
     * Scrolls the current screen content to the bottom.
     * Requires accessibility service to be enabled.
     */
    suspend fun scrollToBottom(): Result<String> = sendCommand(COMMAND_SCROLL_TO_BOTTOM)

    /**
     * Clicks a UI element identified by its content description.
     * Requires accessibility service to be enabled.
     *
     * @param description The content description or partial description of the button/element.
     */
    suspend fun clickButton(description: String): Result<String> {
        return sendCommand(COMMAND_CLICK_BY_DESC, description)
    }

    /**
     * Clicks a UI element identified by its visible text.
     * Requires accessibility service to be enabled.
     */
    suspend fun clickByText(text: String): Result<String> {
        return sendCommand(COMMAND_CLICK_BY_TEXT, text)
    }

    /**
     * Retrieves the text content of the currently visible screen.
     * Requires accessibility service to be enabled.
     *
     * @return [Result] with the screen text content.
     */
    suspend fun getScreenText(): Result<String> = sendCommand(COMMAND_GET_SCREEN_TEXT)

    // ── Internal Communication ──────────────────────────────────────────

    /**
     * Sends a command to the AccessibilityService via a LocalBroadcast
     * (or regular broadcast as fallback) and waits for a response.
     */
    private suspend fun sendCommand(command: String, param: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            if (!isAccessibilityEnabled()) {
                return@withContext Result.Error(
                    IllegalStateException("Accessibility service not enabled"),
                    "Screen automation requires TARZO's Accessibility Service to be enabled. " +
                        "Please go to Settings > Accessibility > TARZO and enable it."
                )
            }

            val requestId = nextRequestId()
            val resultRef = AtomicReference<Result<String>?>(null)
            val latch = CountDownLatch(1)

            // Register a receiver for the result
            val resultReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val rid = intent.getLongExtra(EXTRA_REQUEST_ID, -1)
                    if (rid != requestId) return

                    val success = intent.getBooleanExtra(EXTRA_SUCCESS, false)
                    val data = intent.getStringExtra(EXTRA_RESULT_DATA) ?: ""

                    resultRef.set(
                        if (success) Result.Success(data)
                        else Result.Error(IllegalStateException(data), data)
                    )
                    latch.countDown()
                }
            }

            val resultFilter = IntentFilter(ACTION_RESULT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(resultReceiver, resultFilter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(resultReceiver, resultFilter)
            }

            try {
                // Send the command
                val commandIntent = Intent(ACTION_SCREEN_AUTOMATION).apply {
                    putExtra(EXTRA_COMMAND, command)
                    putExtra(EXTRA_PARAM, param)
                    putExtra(EXTRA_REQUEST_ID, requestId)
                    `package` = context.packageName
                }
                context.sendBroadcast(commandIntent)

                // Wait for response with timeout
                val received = latch.await(5, TimeUnit.SECONDS)
                if (!received) {
                    Result.Error(
                        java.util.concurrent.TimeoutException("Accessibility service did not respond"),
                        "Screen automation timed out. Make sure TARZO's accessibility service is running."
                    )
                } else {
                    resultRef.get() ?: Result.Error(
                        IllegalStateException("No result received"),
                        "Screen automation failed to produce a result."
                    )
                }
            } catch (e: Exception) {
                Result.Error(e, "Screen automation error: ${e.message}")
            } finally {
                try { context.unregisterReceiver(resultReceiver) } catch (_: Exception) {}
            }
        }
}

