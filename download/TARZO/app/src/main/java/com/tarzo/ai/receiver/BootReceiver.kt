package com.tarzo.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tarzo.ai.TarzoApp
import com.tarzo.ai.services.AntiTheftService
import com.tarzo.ai.services.WakeWordService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for BOOT_COMPLETED.
 *
 * Restarts [WakeWordService] and [AntiTheftService] if they
 * were enabled before the device rebooted.
 *
 * This ensures that TARZO's always-on features survive a reboot.
 *
 * Manifest registration:
 * ```
 * <receiver android:name=".receiver.BootReceiver" android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.intent.action.BOOT_COMPLETED" />
 *         <action android:name="android.intent.action.QUICKBOOT_POWERON" />
 *     </intent-filter>
 * </receiver>
 * ```
 *
 * Requires [android.Manifest.permission.RECEIVE_BOOT_COMPLETED].
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i(TAG, "Device booted. Restoring TARZO services...")
            restoreServices(context)
        }
    }

    private fun restoreServices(context: Context) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                // Wait briefly for the system to fully initialize
                kotlinx.coroutines.delay(3000L)

                val prefs = TarzoApp.instance.dataStore.data.first()

                val wakeWordEnabled = prefs[TarzoApp.KEY_WAKE_WORD_ENABLED]?.toBoolean() ?: true
                if (wakeWordEnabled) {
                    Log.i(TAG, "Wake word was enabled before reboot. Restarting WakeWordService.")
                    WakeWordService.start(context)
                }

                val antiTheftEnabled = prefs[TarzoApp.KEY_ANTI_THEFT_ENABLED]?.toBoolean() ?: false
                if (antiTheftEnabled) {
                    Log.i(TAG, "Anti-theft was enabled before reboot. Restarting AntiTheftService.")
                    AntiTheftService.start(context)
                }

                Log.i(TAG, "Service restoration complete.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore services: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
