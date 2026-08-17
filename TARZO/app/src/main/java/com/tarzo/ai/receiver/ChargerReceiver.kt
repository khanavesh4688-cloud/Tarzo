package com.tarzo.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tarzo.ai.TarzoApp
import com.tarzo.ai.services.AntiTheftService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for charger connect/disconnect events.
 *
 * Triggers the anti-theft alarm if anti-theft mode is enabled
 * and the charger is disconnected while the device was charging.
 *
 * This receiver works independently of [AntiTheftService] — it provides
 * a fallback trigger when the service's internal charger receiver might
 * not be active (e.g., service was killed by the OS).
 *
 * Manifest registration:
 * ```
 * <receiver android:name=".receiver.ChargerReceiver" android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.intent.action.ACTION_POWER_CONNECTED" />
 *         <action android:name="android.intent.action.ACTION_POWER_DISCONNECTED" />
 *     </intent-filter>
 * </receiver>
 * ```
 */
class ChargerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                Log.d(TAG, "Charger connected")
                saveChargingState(context, isCharging = true)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d(TAG, "Charger disconnected")
                saveChargingState(context, isCharging = false)
                handleChargerDisconnect(context)
            }
        }
    }

    private fun handleChargerDisconnect(context: Context) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val prefs = TarzoApp.instance.dataStore.data.first()
                val antiTheftEnabled = prefs["TarzoApp.KEY_ANTI_THEFT_ENABLED"]?.toBoolean() ?: false
                val wasCharging = prefs[KEY_WAS_CHARGING]?.toBoolean() ?: false

                if (antiTheftEnabled && wasCharging) {
                    Log.w(TAG, "Anti-theft enabled and charger disconnected! Triggering alarm.")
                    AntiTheftService.triggerAlarm(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check anti-theft state: ${e.message}")
            }
        }
    }

    private fun saveChargingState(context: Context, isCharging: Boolean) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                TarzoApp.instance.dataStore.edit { prefs ->
                    prefs[KEY_WAS_CHARGING] = isCharging.toString()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save charging state: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "ChargerReceiver"
        private val KEY_WAS_CHARGING = stringPreferencesKey("was_charging_before_disconnect")
    }
}
