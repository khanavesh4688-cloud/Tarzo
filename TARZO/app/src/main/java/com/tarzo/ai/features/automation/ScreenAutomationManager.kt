package com.tarzo.ai.features.automation

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
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
 * Provides full screen automation via TARZO AccessibilityService.
 *
 * Capabilities:
 * - Navigate: back, home, recents, notifications
 * - Click / Long-press any element by text or description
 * - Type text into focused input fields
 * - Scroll up/down
 * - Swipe in any direction
 * - Read full screen text
 * - Get current app/activity name
 *
 * Requires: Settings > Accessibility > TARZO enabled
 */
@Singleton
class ScreenAutomationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val ACTION_SCREEN_AUTOMATION = "com.tarzo.ai.ACTION_SCREEN_AUTOMATION"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_PARAM = "param"
        const val EXTRA_PARAM2 = "param2"
        const val EXTRA_REQUEST_ID = "request_id"
        const val ACTION_RESULT = "com.tarzo.ai.ACTION_SCREEN_RESULT"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_SUCCESS = "success"

        // Navigation
        const val COMMAND_BACK = "back"
        const val COMMAND_HOME = "home"
        const val COMMAND_RECENTS = "recents"
        const val COMMAND_NOTIFICATIONS = "notifications"

        // Scroll
        const val COMMAND_SCROLL_UP = "scroll_up"
        const val COMMAND_SCROLL_DOWN = "scroll_down"

        // Click
        const val COMMAND_CLICK_BY_DESC = "click_by_description"
        const val COMMAND_CLICK_BY_TEXT = "click_by_text"
        const val COMMAND_LONG_PRESS_TEXT = "long_press_text"

        // Input
        const val COMMAND_TYPE_TEXT = "type_text"

        // Gesture
        const val COMMAND_SWIPE = "swipe"

        // Info
        const val COMMAND_GET_SCREEN_TEXT = "get_screen_text"
        const val COMMAND_GET_CURRENT_APP = "get_current_app"
        const val COMMAND_PERFORM_ACTION = "perform_action"

        private var requestIdCounter = 0L
        private fun nextRequestId(): Long = ++requestIdCounter
    }

    fun isAccessibilityEnabled(): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return false
            val service = ComponentName(context, TarzoAccessibilityService::class.java)
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
        } catch (_: Exception) {
            try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                    ?.contains(context.packageName) == true
            } catch (_: Exception) { false }
        }
    }

    fun openAccessibilitySettings(): Result<Unit> {
        return try {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Could not open accessibility settings")
        }
    }

    // ── High-level API ──────────────────────────────────────

    suspend fun goBack(): Result<String> = sendCommand(COMMAND_BACK)
    suspend fun goHome(): Result<String> = sendCommand(COMMAND_HOME)
    suspend fun openRecents(): Result<String> = sendCommand(COMMAND_RECENTS)
    suspend fun openNotifications(): Result<String> = sendCommand(COMMAND_NOTIFICATIONS)
    suspend fun scrollUp(): Result<String> = sendCommand(COMMAND_SCROLL_UP)
    suspend fun scrollDown(): Result<String> = sendCommand(COMMAND_SCROLL_DOWN)
    suspend fun clickButton(desc: String): Result<String> = sendCommand(COMMAND_CLICK_BY_DESC, desc)
    suspend fun clickByText(text: String): Result<String> = sendCommand(COMMAND_CLICK_BY_TEXT, text)
    suspend fun longPress(text: String): Result<String> = sendCommand(COMMAND_LONG_PRESS_TEXT, text)
    suspend fun typeText(text: String): Result<String> = sendCommand(COMMAND_TYPE_TEXT, text)
    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int): Result<String> =
        sendCommand(COMMAND_SWIPE, "$x1,$y1,$x2,$y2")
    suspend fun getScreenText(): Result<String> = sendCommand(COMMAND_GET_SCREEN_TEXT)
    suspend fun getCurrentApp(): Result<String> = sendCommand(COMMAND_GET_CURRENT_APP)

    // ── Internal Communication ──────────────────────────────

    private suspend fun sendCommand(command: String, param: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            if (!isAccessibilityEnabled()) {
                return@withContext Result.Error(
                    IllegalStateException("Accessibility not enabled"),
                    "TARZO Accessibility Service chalu nahi hai. Settings > Accessibility > TARZO ko on karo."
                )
            }

            val requestId = nextRequestId()
            val resultRef = AtomicReference<Result<String>?>(null)
            val latch = CountDownLatch(1)

            val resultReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.getLongExtra(EXTRA_REQUEST_ID, -1) != requestId) return
                    val success = intent.getBooleanExtra(EXTRA_SUCCESS, false)
                    val data = intent.getStringExtra(EXTRA_RESULT_DATA) ?: ""
                    resultRef.set(if (success) Result.Success(data) else Result.Error(IllegalStateException(data), data))
                    latch.countDown()
                }
            }

            val filter = IntentFilter(ACTION_RESULT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(resultReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(resultReceiver, filter)
            }

            try {
                context.sendBroadcast(Intent(ACTION_SCREEN_AUTOMATION).apply {
                    putExtra(EXTRA_COMMAND, command)
                    putExtra(EXTRA_PARAM, param)
                    putExtra(EXTRA_REQUEST_ID, requestId)
                    `package` = context.packageName
                })

                val received = latch.await(5, TimeUnit.SECONDS)
                if (!received) {
                    Result.Error(java.util.concurrent.TimeoutException(), "Screen automation timed out")
                } else {
                    resultRef.get() ?: Result.Error(IllegalStateException("No result"), "Failed")
                }
            } catch (e: Exception) {
                Result.Error(e, "Screen automation error: ${e.message}")
            } finally {
                try { context.unregisterReceiver(resultReceiver) } catch (_: Exception) {}
            }
        }
}
