package com.tarzo.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.tarzo.ai.services.CallDetectionService

/**
 * BroadcastReceiver for call state changes.
 *
 * Works with [CallDetectionService] to detect incoming calls.
 * When a call state change is detected, forwards the information
 * to [CallDetectionService] for processing (TTS announcement, etc.).
 *
 * This receiver can also work standalone if CallDetectionService is not running.
 *
 * Requires [android.Manifest.permission.READ_PHONE_STATE].
 */
class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                handlePhoneStateChange(context, intent)
            }
        }
    }

    private fun handlePhoneStateChange(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                Log.i(TAG, "Incoming call from: $incomingNumber")
                onIncomingCall(context, incomingNumber)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                Log.d(TAG, "Call answered/off-hook")
                onCallStarted(context)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                Log.d(TAG, "Call ended/idle")
                onCallEnded(context)
            }
        }
    }

    private fun onIncomingCall(context: Context, phoneNumber: String?) {
        // If CallDetectionService is running, it handles the announcement
        // via its own PhoneStateListener. This receiver acts as a backup
        // and for standalone use.

        val callerName = if (
            context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            lookupContactName(context, phoneNumber)
        } else null

        val displayName = callerName ?: phoneNumber ?: "Unknown number"
        Log.i(TAG, "Call from: $displayName")

        // Send a broadcast that CallDetectionService or other components can listen to
        val announcementIntent = Intent(ACTION_INCOMING_CALL).apply {
            `package` = context.packageName
            putExtra(EXTRA_CALLER_NAME, displayName)
            putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
        }
        try {
            context.sendBroadcast(announcementIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send call announcement: ${e.message}")
        }
    }

    private fun onCallStarted(context: Context) {
        val intent = Intent(ACTION_CALL_STARTED).apply {
            `package` = context.packageName
        }
        try { context.sendBroadcast(intent) } catch (_: Exception) {}
    }

    private fun onCallEnded(context: Context) {
        val intent = Intent(ACTION_CALL_ENDED).apply {
            `package` = context.packageName
        }
        try { context.sendBroadcast(intent) } catch (_: Exception) {}
    }

    private fun lookupContactName(context: Context, phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null
        return try {
            val uri = android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(phoneNumber)
                .build()
            val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Contact lookup failed: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "CallStateReceiver"

        // Internal broadcast actions for inter-component communication
        const val ACTION_INCOMING_CALL = "com.tarzo.ai.action.INCOMING_CALL"
        const val ACTION_CALL_STARTED = "com.tarzo.ai.action.CALL_STARTED"
        const val ACTION_CALL_ENDED = "com.tarzo.ai.action.CALL_ENDED"
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }
}
