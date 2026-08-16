package com.tarzo.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tarzo.ai.TarzoApp
import com.tarzo.ai.core.voice.TTSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for incoming SMS messages.
 *
 * When an SMS is received, optionally announces the sender via TTS.
 * The announcement can be enabled/disabled via DataStore preferences.
 *
 * Requires [android.Manifest.permission.RECEIVE_SMS] and
 * [android.Manifest.permission.READ_SMS] in the manifest.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val messages = extractSmsMessages(intent)
        if (messages.isEmpty()) return

        val sender = messages.firstOrNull()?.displayOriginatingAddress ?: "Unknown"
        val body = messages.joinToString("") { it.displayMessageBody ?: "" }

        Log.i(TAG, "SMS received from: $sender, body: ${body.take(50)}...")

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope.launch {
            try {
                val prefs = TarzoApp.instance.dataStore.data.first()
                val smsAnnounceEnabled = prefs[KEY_SMS_ANNOUNCE]?.toBoolean() ?: false

                if (smsAnnounceEnabled) {
                    val contactName = lookupContactName(context, sender)
                    val displayName = contactName ?: formatPhoneNumber(sender)
                    val ttsManager = TTSManager(context.applicationContext)
                    ttsManager.initialize()
                    val preview = body.take(30).replace("\n", " ")
                    val announcement = "$displayName ka message aaya hai. $preview"
                    ttsManager.speak(announcement)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to announce SMS: ${e.message}")
            }
        }
    }

    private fun extractSmsMessages(intent: Intent): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val bundle = intent.extras ?: return messages

        val pdus = bundle.get("pdus") as? Array<*> ?: return messages
        val format = bundle.getString("format")

        for (pdu in pdus) {
            val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                SmsMessage.createFromPdu(pdu as? ByteArray, format)
            } else {
                @Suppress("DEPRECATION")
                SmsMessage.createFromPdu(pdu as? ByteArray)
            }
            smsMessage?.let { messages.add(it) }
        }
        return messages
    }

    private fun lookupContactName(context: Context, phoneNumber: String): String? {
        if (!hasPermission(context, android.Manifest.permission.READ_CONTACTS)) return null
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

    private fun formatPhoneNumber(number: String): String {
        val digits = number.replace(Regex("[^0-9]"), "")
        return when {
            digits.length == 10 -> "${digits.substring(0, 4)} ${digits.substring(4)}"
            digits.length > 10 -> "+${digits.substring(0, digits.length - 10)} ${digits.substring(digits.length - 10, digits.length - 6)} ${digits.substring(digits.length - 6)}"
            else -> number
        }
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "SmsReceiver"
        private val KEY_SMS_ANNOUNCE = stringPreferencesKey("sms_announce_enabled")
    }
}
