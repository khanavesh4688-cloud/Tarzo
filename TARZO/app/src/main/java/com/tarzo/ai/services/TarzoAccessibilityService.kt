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
import com.tarzo.ai.features.automation.ScreenAutomationManager

/**
 * AccessibilityService for TARZO screen automation.
 *
 * Maintains a static instance reference so that [ScreenAutomationManager]
 * can communicate with it via broadcasts. Implements [onAccessibilityEvent]
 * to detect UI nodes, extract text from the screen, perform clicks on
 * matched nodes, and perform scroll actions.
 *
 * This service must be enabled by the user in:
 * Settings > Accessibility > TARZO
 *
 * Requires an accessibility service configuration XML file:
 * ```xml
 * <accessibility-service
 *     xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
 *     android:accessibilityFeedbackType="feedbackGeneric"
 *     android:canRetrieveWindowContent="true"
 *     android:notificationTimeout="100"
 *     android:settingsActivity="com.tarzo.ai.MainActivity"
 *     android:description="@string/accessibility_service_description" />
 * ```
 */
class TarzoAccessibilityService : AccessibilityService() {

    private var commandReceiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
            packageNames = null // Observe all packages
        }

        registerCommandReceiver()
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val className = event.className?.toString() ?: ""
                Log.d(TAG, "Window changed: $className")
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Screen content changed, useful for monitoring
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
        Log.d(TAG, "Accessibility service destroyed")
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
        Log.d(TAG, "Command receiver registered")
    }

    private fun unregisterCommandReceiver() {
        try {
            commandReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {
        }
        commandReceiver = null
    }

    private fun handleCommand(intent: Intent) {
        val command = intent.getStringExtra(ScreenAutomationManager.EXTRA_COMMAND) ?: return
        val param = intent.getStringExtra(ScreenAutomationManager.EXTRA_PARAM)
        val requestId = intent.getLongExtra(ScreenAutomationManager.EXTRA_REQUEST_ID, -1)

        val root = rootInActiveWindow
        if (root == null) {
            sendResult(requestId, false, "No active window found. Is the screen on?")
            return
        }

        val (success, message) = when (command) {
            ScreenAutomationManager.COMMAND_SCROLL_UP -> {
                performScroll(root, direction = ScrollDirection.UP)
            }
            ScreenAutomationManager.COMMAND_SCROLL_DOWN -> {
                performScroll(root, direction = ScrollDirection.DOWN)
            }
            ScreenAutomationManager.COMMAND_SCROLL_TO_TOP -> {
                performScrollToTop(root)
            }
            ScreenAutomationManager.COMMAND_SCROLL_TO_BOTTOM -> {
                performScrollToBottom(root)
            }
            ScreenAutomationManager.COMMAND_CLICK_BY_DESC -> {
                performClickByDescription(root, param)
            }
            ScreenAutomationManager.COMMAND_CLICK_BY_TEXT -> {
                performClickByText(root, param)
            }
            ScreenAutomationManager.COMMAND_GET_SCREEN_TEXT -> {
                val text = getScreenText(root)
                (text.isNotEmpty()) to (text.ifEmpty { "No text found on screen." })
            }
            else -> false to "Unknown command: $command"
        }

        sendResult(requestId, success, message)
    }

    // ── Node Search Helpers ───────────────────────────────────────

    /**
     * Recursively finds all nodes whose text contains [text].
     */
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
            val child = node.getChild(i) ?: continue
            findNodesByTextRecursive(child, text, result)
        }
    }

    /**
     * Recursively finds all nodes whose content description contains [description].
     */
    fun findNodeByDescription(
        root: AccessibilityNodeInfo,
        description: String
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        findNodesByDescriptionRecursive(root, description, result)
        return result
    }

    private fun findNodesByDescriptionRecursive(
        node: AccessibilityNodeInfo,
        description: String,
        result: MutableList<AccessibilityNodeInfo>
    ) {
        val desc = node.contentDescription?.toString() ?: ""
        if (desc.contains(description, ignoreCase = true)) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findNodesByDescriptionRecursive(child, description, result)
        }
    }

    // ── Scroll Actions ────────────────────────────────────────────

    enum class ScrollDirection { UP, DOWN }

    /**
     * Performs a scroll on the first scrollable node found in the tree.
     */
    fun performScroll(
        root: AccessibilityNodeInfo,
        direction: ScrollDirection = ScrollDirection.DOWN
    ): Pair<Boolean, String> {
        val scrollableNodes = findScrollableNodes(root)
        for (node in scrollableNodes) {
            val action = if (direction == ScrollDirection.DOWN) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            if (node.performAction(action)) {
                val dirLabel = if (direction == ScrollDirection.DOWN) "down" else "up"
                return true to "Scrolled $dirLabel"
            }
        }
        return false to "No scrollable container found"
    }

    private fun performScrollToTop(root: AccessibilityNodeInfo): Pair<Boolean, String> {
        var scrolled = false
        val nodes = findScrollableNodes(root)
        for (node in nodes) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
                scrolled = true
            }
        }
        return scrolled to (if (scrolled) "Scrolled to top" else "No scrollable container found")
    }

    private fun performScrollToBottom(root: AccessibilityNodeInfo): Pair<Boolean, String> {
        var scrolled = false
        val nodes = findScrollableNodes(root)
        for (node in nodes) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                scrolled = true
            }
        }
        return scrolled to (if (scrolled) "Scrolled to bottom" else "No scrollable container found")
    }

    private fun findScrollableNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        findScrollableNodesRecursive(root, result)
        return result.ifEmpty { listOf(root) }
    }

    private fun findScrollableNodesRecursive(
        node: AccessibilityNodeInfo,
        result: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.isScrollable) {
            result.add(node)
        }
        val actions = node.actions
        if (actions != null && actions?.any { it == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD } == true) {
            if (!result.contains(node)) {
                result.add(node)
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findScrollableNodesRecursive(child, result)
        }
    }

    // ── Click Actions ─────────────────────────────────────────────

    private fun performClickByDescription(
        root: AccessibilityNodeInfo,
        description: String?
    ): Pair<Boolean, String> {
        if (description.isNullOrBlank()) return false to "No description provided"
        val nodes = findNodeByDescription(root, description)
        for (node in nodes) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true to "Clicked: $description"
            }
            // Try clicking the parent
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true to "Clicked parent of: $description"
                }
                parent = parent.parent
            }
        }
        return false to "Could not find clickable element with description: $description"
    }

    private fun performClickByText(
        root: AccessibilityNodeInfo,
        text: String?
    ): Pair<Boolean, String> {
        if (text.isNullOrBlank()) return false to "No text provided"
        val nodes = findNodeByText(root, text)
        for (node in nodes) {
            if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true to "Clicked: $text"
            }
            // Try clicking the parent
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true to "Clicked parent of: $text"
                }
                parent = parent.parent
            }
        }
        return false to "Could not find clickable element with text: $text"
    }

    // ── Screen Text Extraction ────────────────────────────────────

    /**
     * Extracts all visible text from the current screen recursively.
     */
    fun getScreenText(root: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        collectTextRecursive(root, sb)
        return sb.toString().trim()
    }

    private fun collectTextRecursive(node: AccessibilityNodeInfo, sb: StringBuilder) {
        node.text?.toString()?.let { text ->
            if (text.isNotBlank()) sb.appendLine(text.trim())
        }
        node.contentDescription?.toString()?.let { desc ->
            if (desc.isNotBlank()) sb.appendLine(desc.trim())
        }
        // Also collect hint text
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()?.let { hint ->
                if (hint.isNotBlank()) sb.appendLine("[hint: ${hint.trim()}]")
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTextRecursive(child, sb)
        }
    }

    // ── Result Sending ────────────────────────────────────────────

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

        /**
         * Static instance reference for external communication.
         * [ScreenAutomationManager] uses this to check service availability.
         */
        @Volatile
        var instance: TarzoAccessibilityService? = null
            private set
    }
}
