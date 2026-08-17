package com.tarzo.ai.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import javax.inject.Inject

data class PermissionGroup(
    val name: String,
    val permissions: List<String>,
    val rationale: String,
    val required: Boolean = true
)

class PermissionManager @Inject constructor(
    private val context: Context
) {

    val requiredPermissionGroups: List<PermissionGroup> = listOf(
        PermissionGroup(
            name = "Microphone",
            permissions = listOf(Manifest.permission.RECORD_AUDIO),
            rationale = "TARZO ko aapki awaaz sunne ke liye microphone access chahiye. Bina iske voice commands kaam nahi karenge.",
            required = true
        ),
        PermissionGroup(
            name = "Camera",
            permissions = listOf(Manifest.permission.CAMERA),
            rationale = "Photo, selfie aur video recording ke liye camera access chahiye.",
            required = false
        ),
        PermissionGroup(
            name = "Phone",
            permissions = listOf(
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS
            ),
            rationale = "Calls karni ho toh phone aur contacts access chahiye.",
            required = false
        ),
        PermissionGroup(
            name = "SMS",
            permissions = listOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_SMS
            ),
            rationale = "SMS bhejne ke liye permission chahiye.",
            required = false
        ),
        PermissionGroup(
            name = "Storage",
            permissions = buildList {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                    add(Manifest.permission.READ_MEDIA_VIDEO)
                    add(Manifest.permission.READ_MEDIA_AUDIO)
                } else {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            },
            rationale = "Files aur media access ke liye storage permission chahiye.",
            required = false
        ),
        PermissionGroup(
            name = "Notifications",
            permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyList()
            },
            rationale = "Notifications dikhane ke liye permission chahiye.",
            required = false
        ),
        PermissionGroup(
            name = "Bluetooth",
            permissions = buildList {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(Manifest.permission.BLUETOOTH_SCAN)
                    add(Manifest.permission.BLUETOOTH_ADVERTISE)
                }
            },
            rationale = "Bluetooth control karne ke liye permission chahiye.",
            required = false
        ),
        PermissionGroup(
            name = "Overlay",
            permissions = listOf(Manifest.permission.SYSTEM_ALERT_WINDOW),
            rationale = "Floating voice assistant overlay dikhane ke liye permission chahiye.",
            required = false
        ),
        PermissionGroup(
            name = "Alarm",
            permissions = listOf(
                Manifest.permission.SCHEDULE_EXACT_ALARM,
                Manifest.permission.USE_EXACT_ALARM
            ),
            rationale = "Alarm aur timer set karne ke liye permission chahiye.",
            required = false
        )
    )

    val allPermissions: List<String>
        get() = requiredPermissionGroups.flatMap { it.permissions }.distinct()

    val requiredPermissions: List<String>
        get() = requiredPermissionGroups
            .filter { it.required }
            .flatMap { it.permissions }
            .distinct()

    fun getMissingPermissions(): List<String> {
        return allPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun getMissingRequiredPermissions(): List<String> {
        return requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAllPermissions(): Boolean {
        return getMissingRequiredPermissions().isEmpty()
    }
    fun hasAllOptionalPermissions(): Boolean {
        return getMissingPermissions().isEmpty()
    }

    fun getPermissionGroup(permission: String): PermissionGroup? {
        return requiredPermissionGroups.find { permission in it.permissions }
    }

    fun getRationaleForPermission(permission: String): String {
        return getPermissionGroup(permission)?.rationale ?: "This permission is needed."
    }

    fun getMissingPermissionGroups(): List<PermissionGroup> {
        return requiredPermissionGroups.filter { group ->
            group.permissions.any { !hasPermission(it) }
        }
    }

    companion object {
        @Volatile
        private var instance: PermissionManager? = null

        fun getInstance(context: Context): PermissionManager {
            return instance ?: synchronized(this) {
                instance ?: PermissionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * Compose helper to check and request permissions.
 */
@Composable
fun rememberPermissionState(
    permissionManager: PermissionManager
): PermissionState {
    val missingPermissions = remember {
        mutableStateListOf<String>().apply {
            addAll(permissionManager.getMissingPermissions())
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        for ((permission, granted) in results) {
            if (granted) {
                missingPermissions.remove(permission)
            }
        }
    }

    LaunchedEffect(Unit) {
        val toRequest = permissionManager.getMissingPermissions().toTypedArray()
        if (toRequest.isNotEmpty()) {
            launcher.launch(toRequest)
        }
    }

    return PermissionState(
        allGranted = missingPermissions.isEmpty(),
        missingPermissions = missingPermissions.toList(),
        requestPermissions = {
            val toRequest = permissionManager.getMissingPermissions().toTypedArray()
            if (toRequest.isNotEmpty()) {
                launcher.launch(toRequest)
            }
        },
        shouldShowRationale = permissionManager.getMissingPermissions().any {
            !permissionManager.hasPermission(it)
        }
    )
}

data class PermissionState(
    val allGranted: Boolean,
    val missingPermissions: List<String>,
    val requestPermissions: () -> Unit,
    val shouldShowRationale: Boolean
)