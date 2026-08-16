package com.tarzo.ai.features.communication

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.SmsManager as AndroidSmsManager
import androidx.core.content.ContextCompat
import com.tarzo.ai.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends SMS messages using Android's [android.telephony.SmsManager].
 *
 * Important: Sending SMS always requires user confirmation.
 * The [SmsManager] provides a pending message state that the UI must
 * confirm before actually sending. This is a safety measure to prevent
 * unauthorized SMS from being sent by voice commands.
 *
 * Requires [Manifest.permission.SEND_SMS] and optionally
 * [Manifest.permission.READ_CONTACTS] for name-based number lookups.
 */
@Singleton
class SmsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** A message awaiting user confirmation before sending. */
    data class PendingSms(
        val number: String,
        val message: String,
        val contactName: String?
 = null
    )

    private val _pendingSms = MutableStateFlow<PendingSms?>(null)
    val pendingSms: StateFlow<PendingSms?> = _pendingSms.asStateFlow()

    /**
     * Prepares an SMS for sending. Stores it as a pending message
     * that must be confirmed via [confirmAndSend] or cancelled via [cancelPending].
     *
     * @param number The destination phone number.
     * @param message The text message body.
     * @return [Result] confirming the message is staged for review.
     */
    fun prepareSms(number: String, message: String): Result<String> {
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            return Result.Error(
                SecurityException("SEND_SMS permission not granted"),
                "SMS permission is required. Please grant it in settings."
            )
        }
        val sanitized = number.filter { it.isDigit() || it == '+' }
        if (sanitized.length < 3) {
            return Result.Error(
                IllegalArgumentException("Invalid phone number: $number"),
                "'$number' is not a valid phone number."
            )
        }
        _pendingSms.value = PendingSms(
            number = sanitized,
            message = message
        )
        return Result.Success("SMS to $sanitized is ready. Please confirm to send.")
    }

    /**
     * Looks up a contact's phone number and prepares an SMS.
     *
     * @param name The contact name to look up.
     * @param message The message body.
     * @return [Result] indicating success or failure of the lookup/prepare step.
     */
    fun prepareSmsToContact(name: String, message: String): Result<String> {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            return Result.Error(
                SecurityException("READ_CONTACTS permission not granted"),
                "Contact read permission is required to look up numbers."
            )
        }
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            return Result.Error(
                SecurityException("SEND_SMS permission not granted"),
                "SMS permission is required."
            )
        }
        val number = lookupContactNumber(name)
        if (number == null) {
            return Result.Error(
                IllegalArgumentException("Contact not found: $name"),
                "Could not find a contact named '$name'."
            )
        }
        _pendingSms.value = PendingSms(
            number = number,
            message = message,
            contactName = name
        )
        return Result.Success("SMS to $name ($number) is ready. Please confirm to send.")
    }

    /**
     * Confirms and sends the currently pending SMS message.
     * This must only be called after user confirmation.
     *
     * @return [Result] with the sent message parts count, or error.
     */
    suspend fun confirmAndSend(): Result<String> = withContext(Dispatchers.IO) {
        val pending = _pendingSms.value
            ?: return@withContext Result.Error(
                IllegalStateException("No pending SMS"),
                "No message is pending for confirmation."
            )

        try {
            if (!hasPermission(Manifest.permission.SEND_SMS)) {
                return@withContext Result.Error(
                    SecurityException("SEND_SMS permission not granted"),
                    "SMS permission was revoked."
                )
            }

            val sms: AndroidSmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(AndroidSmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                AndroidSmsManager.getDefault()
            }

            // For long messages, use MMS or multipart
            val parts = sms.divideMessage(pending.message)
            val sentIntents = parts.mapIndexed { index, _ ->
                PendingIntent.getBroadcast(
                    context,
                    SMS_SENT_REQUEST_CODE + index,
                    Intent(ACTION_SMS_SENT).apply {
                        putExtra("index", index)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            if (parts.size == 1) {
                sms.sendTextMessage(
                    pending.number,
                    null,
                    pending.message,
                    sentIntents.first(),
                    null
                )
            } else {
                sms.sendMultipartTextMessage(
                    pending.number,
                    null,
                    parts,
                    sentIntents.toMutableList(),
                    null
                )
            }

            val recipient = pending.contactName ?: pending.number
            _pendingSms.value = null
            Result.Success("SMS sent to $recipient with ${parts.size} part(s).")
        } catch (e: Exception) {
            Result.Error(e, "Failed to send SMS: ${e.message}")
        }
    }

    /**
     * Cancels the currently pending SMS without sending it.
     */
    fun cancelPending() {
        _pendingSms.value = null
    }

    /**
     * Opens the default SMS app with a pre-filled number and message.
     * This does NOT require SEND_SMS permission as it delegates to the SMS app.
     */
    fun openSmsApp(number: String, message: String = ""): Result<Unit> {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${number.filter { it.isDigit() || it == '+' }}")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Could not open SMS app: ${e.message}")
        }
    }

    /**
     * Looks up a contact's phone number by display name.
     */
    fun getContactNumber(name: String): String? {
        return lookupContactNumber(name)
    }

    /**
     * Internal contact lookup using ContentResolver.
     */
    private fun lookupContactNumber(name: String): String? {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) return null

        val normalizedQuery = name.lowercase().trim()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$normalizedQuery%")

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED + " DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
            }
        }
        return null
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val SMS_SENT_REQUEST_CODE = 2001
        private const val ACTION_SMS_SENT = "com.tarzo.ai.SMS_SENT"
    }
}
