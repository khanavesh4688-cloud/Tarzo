package com.tarzo.ai.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect
import com.tarzo.ai.features.automation.ScreenAutomationManager

/**
 * Full-screen AccessibilityService for TARZO.
 *
 * Can: click, long-press, type text, scroll, swipe, go back/home/recent,
 * read screen text, find and click any element.
 *
 * Enable in: Settings > Accessibility > TARZO
 */
class TarzoAccessibilityService : AccessibilityService() {

    private var commandReceiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            notificationTimeout = 50
            packageNames = null // Observe ALL packages
        }

        registerCommandReceiver()
        Log.i(TAG, "TARZO Accessibility service connected - full access enabled")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                currentApp = event.packageName?.toString() ?: ""
                currentActivity = event.className?.toString() ?: ""
                Log.d(TAG, "App: $currentApp | Activity: $currentActivity")
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                Log.d(TAG, "Click: ${event.source?.text} in ${event.packageName}")
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        unregisterCommandReceiver()
        instance = null
        super.onDestroy()
    }

    // ── Global Actions (Back, Home, Recent, Power) ──────────

    fun performBack(): Boolean {
        val result = performGlobalAction(GLOBAL_ACTION_BACK)
        Log.d(TAG, "Back: $result")
        return result
    }

    fun performHome(): Boolean {
        val result = performGlobalAction(GLOBAL_ACTION_HOME)
        Log.d(TAG, "Home: $result")
        return result
    }

    fun performRecents(): Boolean {
        val result = performGlobalAction(GLOBAL_ACTION_RECENTS)
        Log.d(TAG, "Recents: $result")
        return result
    }

    fun performNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            false
        } else {
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }
    }

    fun performQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun performPowerDialog(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        } else false
    }

    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else false
    }

    fun takeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else false
    }

    // ── Gesture Actions (Swipe, Click, Long Press) ───────────

    fun performClick(x: Int, y: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = android.graphics.Path().apply { moveTo(x.toFloat(), y.toFloat()) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                .build()
            dispatchGesture(gesture, null, null)
            Log.d(TAG, "Click at ($x, $y)")
            true
        } else {
            false
        }
    }

    fun performLongClick(x: Int, y: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = android.graphics.Path().apply { moveTo(x.toFloat(), y.toFloat()) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
                .build()
            dispatchGesture(gesture, null, null)
            Log.d(TAG, "Long click at ($x, $y)")
            true
        } else {
            false
        }
    }

    fun performSwipe(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        durationMs: Long = 300
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = android.graphics.Path().apply {
                moveTo(startX.toFloat(), startY.toFloat())
                lineTo(endX.toFloat(), endY.toFloat())
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build()
            dispatchGesture(gesture, null, null)
            Log.d(TAG, "Swipe ($startX,$startY) -> ($endX,$endY)")
            true
        } else {
            false
        }
    }

    // ── Text Input ─────────────────────────────────────────────

    fun typeText(text: String): Boolean {
        val focused = findFocusedInput()
        if (focused != null) {
            val args = Bundle()
            args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
            val result = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            Log.d(TAG, "Type text into focused node: $result")
            return result
        }
        // Fallback: use ACTION_PASTE via clipboard
        Log.w(TAG, "No focused input found for typing")
        return false
    }

    fun pasteText(): Boolean {
        val focused = findFocusedInput()
        if (focused != null) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                focused.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            } else false
        }
        return false
    }

    // ── Node Search ────────────────────────────────────────────

    fun findNodeByText(root: AccessibilityNodeInfo, text: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        findNodesByTextRecursive(root, text, result)
        return result
    }

    private fun findNodesByTextRecursive(
        node: AccessibilityNodeInfo,
        text: String,
        result: MutableList<AccessibilityNodeInfo>
    ) {
        val nodeText = node.text?.toString() ?: ""
        if (nodeText.contains(text, ignoreCase = true)) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findNodesByTextRecursive(it, text, result) }
        }
    }

    fun findNodeByDescription(
        root: AccessibilityNodeInfo,
        description: String
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        findNodesByDescRecursive(root, description, result)
        return result
    }

    private fun findNodesByDescRecursive(
        node: AccessibilityNodeInfo,
        description: String,
        result: MutableList<AccessibilityNodeInfo>
    ) {
        val desc = node.contentDescription?.toString() ?: ""
        if (desc.contains(description, ignoreCase = true)) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findNodesByDescRecursive(it, description, result) }
        }
    }

    fun findNodeById(root: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            return nodes?.firstOrNull()
        }
        return null
    }

    // ── Click on Nodes ─────────────────────────────────────────

    fun clickNodeByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = findNodeByText(root, text)
        for (node in nodes) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            var parent = node.parent
            while (parent != null) {
                if (parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                parent = parent.parent
            }
        }
        return false
    }

    fun clickNodeByDescription(desc: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = findNodeByDescription(root, desc)
        for (node in nodes) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            var parent = node.parent
            while (parent != null) {
                if (parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                parent = parent.parent
            }
        }
        return false
    }

    fun longPressNodeByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = findNodeByText(root, text)
        for (node in nodes) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) return true
            }
        }
        return false
    }

    // ── Scroll ──────────────────────────────────────────────────

    fun scrollUp(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollableNodes = findScrollableNodes(root)
        for (node in scrollableNodes) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) return true
        }
        return false
    }

    fun scrollDown(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollableNodes = findScrollableNodes(root)
        for (node in scrollableNodes) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
        }
        return false
    }

    private fun findScrollableNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        findScrollableRecursive(node, result)
        return result.ifEmpty { listOf(node) }
    }

    private fun findScrollableRecursive(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (node.isScrollable) result.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findScrollableRecursive(it, result) }
        }
    }

    // ── Screen Text ─────────────────────────────────────────────

    fun getScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        collectText(root, sb)
        return sb.toString().trim()
    }

    fun getCurrentAppName(): String = currentApp
    fun getCurrentActivity(): String = currentActivity

    private fun collectText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        node.text?.toString()?.trim()?.let { if (it.isNotBlank()) sb.appendLine(it) }
        node.contentDescription?.toString()?.trim()?.let { if (it.isNotBlank()) sb.appendLine(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()?.trim()?.let { if (it.isNotBlank()) sb.appendLine("[hint: $it]") }
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, sb) }
        }
    }

    private fun findFocusedInput(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findFocusedRecursive(root)
    }

    private fun findFocusedRecursive(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused && node.isEditable) return node
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (node.isFocused && node.text != null) return node
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                val found = findFocusedRecursive(it)
                if (found != null) return found
            }
        }
        return null
    }

    // ── Broadcast Command Receiver ────────────────────────────────

    private fun registerCommandReceiver() {
        if (commandReceiver != null) return
        commandReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                handleCommand(intent)
            }
        }
        val filter = IntentFilter(ScreenAutomationManager.ACTION_SCREEN_AUTOMATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(commandReceiver, filter)
        }
    }

    private fun unregisterCommandReceiver() {
        try { commandReceiver?.let { unregisterReceiver(it) } } catch (_: Exception) {}
        commandReceiver = null
    }

    private fun handleCommand(intent: Intent) {
        val command = intent.getStringExtra(ScreenAutomationManager.EXTRA_COMMAND) ?: return
        val param = intent.getStringExtra(ScreenAutomationManager.EXTRA_PARAM)
        val param2 = intent.getStringExtra(ScreenAutomationManager.EXTRA_PARAM2)
        val requestId = intent.getLongExtra(ScreenAutomationManager.EXTRA_REQUEST_ID, -1)

        val (success, message) = when (command) {
            // Scroll
            ScreenAutomationManager.COMMAND_SCROLL_UP -> scrollUp() to "Scrolled up"
            ScreenAutomationManager.COMMAND_SCROLL_DOWN -> scrollDown() to "Scrolled down"

            // Navigation
            ScreenAutomationManager.COMMAND_BACK -> performBack() to "Went back"
            ScreenAutomationManager.COMMAND_HOME -> performHome() to "Went home"
            ScreenAutomationManager.COMMAND_RECENTS -> performRecents() to "Opened recents"
            ScreenAutomationManager.COMMAND_NOTIFICATIONS -> performNotifications() to "Opened notifications"

            // Click by text/desc
            ScreenAutomationManager.COMMAND_CLICK_BY_TEXT -> {
                clickNodeByText(param ?: "") to "Clicked: $param"
            }
            ScreenAutomationManager.COMMAND_CLICK_BY_DESC -> {
                clickNodeByDescription(param ?: "") to "Clicked: $param"
            }

            // Long press
            ScreenAutomationManager.COMMAND_LONG_PRESS_TEXT -> {
                longPressNodeByText(param ?: "") to "Long pressed: $param"
            }

            // Type text
            ScreenAutomationManager.COMMAND_TYPE_TEXT -> {
                typeText(param ?: "") to "Typed: ${param?.take(30)}"
            }

            // Swipe
            ScreenAutomationManager.COMMAND_SWIPE -> {
                val parts = (param ?: "").split(",").map { it.trim().toIntOrNull() ?: 0 }
                if (parts.size == 4) {
                    performSwipe(parts[0], parts[1], parts[2], parts[3]) to "Swiped"
                } else {
                    false to "Swipe needs 4 coords: x1,y1,x2,y2"
                }
            }

            // Get screen text
            ScreenAutomationManager.COMMAND_GET_SCREEN_TEXT -> {
                val text = getScreenText()
                (text.isNotEmpty()) to text.ifEmpty { "No text found" }
            }

            // Get current app
            ScreenAutomationManager.COMMAND_GET_CURRENT_APP -> {
                true to currentApp
            }

            else -> false to "Unknown command: $command"
        }

        sendResult(requestId, success, message)
    }

    private fun sendResult(requestId: Long, success: Boolean, message: String) {
        val resultIntent = Intent(ScreenAutomationManager.ACTION_RESULT).apply {
            putExtra(ScreenAutomationManager.EXTRA_REQUEST_ID, requestId)
            putExtra(ScreenAutomationManager.EXTRA_SUCCESS, success)
            putExtra(ScreenAutomationManager.EXTRA_RESULT_DATA, message)
            `package` = packageName
        }
        sendBroadcast(resultIntent)
    }

    companion object {
        private const val TAG = "TarzoAccessibility"

        @Volatile
        var instance: TarzoAccessibilityService? = null
            private set

        @Volatile
        var currentApp: String = ""
            private set

        @Volatile
        var currentActivity: String = ""
            private set
    }
}
