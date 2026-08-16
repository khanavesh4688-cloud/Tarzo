package com.tarzo.ai.features.communication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.tarzo.ai.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages phone call operations using ACTION_CALL intent.
 * Looks up contacts by name via the ContentResolver when needed.
 *
 * Requires [Manifest.permission.CALL_PHONE] and optionally
 * [Manifest.permission.READ_CONTACTS] for name-based lookups.
 */
@Singleton
class CallManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Dials a contact by name. First looks up the contact's phone number
     * in the device contacts database, then initiates the call.
     *
     * @param name The display name or partial name of the contact to call.
     * @return [Result] with success message or error.
     */
    suspend fun dialContact(name: String): Result<String> = withContext(Dispatchers.IO) {
        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            return@withContext Result.Error(
                SecurityException("CALL_PHONE permission not granted"),
                "Phone call permission is required. Please grant it in app settings."
            )
        }
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            return@withContext Result.Error(
                SecurityException("READ_CONTACTS permission not granted"),
                "Contact read permission is required to look up contacts. Please grant it in settings."
            )
        }

        val number = lookupContactNumber(name)
        if (number == null) {
            return@withContext Result.Error(
                IllegalArgumentException("Contact not found: $name"),
                "Could not find a contact named '$name' in your contacts."
            )
        }
        dialNumber(number)
    }

    /**
     * Dials a phone number directly.
     *
     * @param number The phone number to call. Should be a valid E.164 or local format number.
     * @return [Result] with success message or error.
     */
    suspend fun dialNumber(number: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission(Manifest.permission.CALL_PHONE)) {
                return@withContext Result.Error(
                    SecurityException("CALL_PHONE permission not granted"),
                    "Phone call permission is required to make calls."
                )
            }

            // Sanitize the number: remove non-digit characters except leading +
            val sanitized = number.filter { it.isDigit() || it == '+' }
            if (sanitized.length < 3) {
                return@withContext Result.Error(
                    IllegalArgumentException("Invalid phone number: $number"),
                    "'$number' does not appear to be a valid phone number."
                )
            }

            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$sanitized")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
            Result.Success("Calling $sanitized")
        } catch (e: SecurityException) {
            Result.Error(e, "Permission denied. Please grant CALL_PHONE permission.")
        } catch (e: Exception) {
            Result.Error(e, "Failed to initiate call: ${e.message}")
        }
    }

    /**
     * Opens the dialer with the given number pre-filled, without actually calling.
     * This does NOT require CALL_PHONE permission.
     */
    fun openDialer(number: String): Result<Unit> {
        return try {
            val sanitized = number.filter { it.isDigit() || it == '+' }
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$sanitized")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Could not open dialer: ${e.message}")
        }
    }

    /**
     * Looks up a contact's phone number by display name.
     * Returns the first matching number, or null if not found.
     */
    fun lookupContactNumber(name: String): String? {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) return null

        val normalizedQuery = name.lowercase().trim()

        var number: String? = null

        // Search by display name
        val displayNameProjection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val displayNameSelection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val displayNameSelectionArgs = arrayOf("%$normalizedQuery%")

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            displayNameProjection,
            displayNameSelection,
            displayNameSelectionArgs,
            ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED + " DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val contactName = cursor.getString(nameIndex)
                // Prefer exact match
                if (contactName.equals(normalizedQuery, ignoreCase = true)) {
                    number = cursor.getString(numberIndex)
                } else if (number == null) {
                    number = cursor.getString(numberIndex)
                }
            }
        }

        return number
    }

    /**
     * Searches contacts by name and returns a list of matching names with their phone numbers.
     */
    fun searchContacts(query: String): List<ContactInfo> {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) return emptyList()

        val results = mutableListOf<ContactInfo>()
        val normalizedQuery = query.lowercase().trim()

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
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                results.add(
                    ContactInfo(
                        name = cursor.getString(nameIndex),
                        number = cursor.getString(numberIndex)
                    )
                )
            }
        }

        return results.distinctBy { it.name.lowercase() }.take(10)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    data class ContactInfo(
        val name: String,
        val number: String
    )
}