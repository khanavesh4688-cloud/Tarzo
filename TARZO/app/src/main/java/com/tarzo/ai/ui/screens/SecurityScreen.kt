package com.tarzo.ai.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tarzo.ai.core.permissions.PermissionGroup
import com.tarzo.ai.ui.components.*
import com.tarzo.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ── Data models ─────────────────────────────────────────────────────────

/** Anti-theft toggle entry. */
data class AntiTheftToggle(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val isEnabled: Boolean,
)

/** Activity log entry. */
data class SecurityLogEntry(
    val id: Long,
    val event: String,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val severity: SecuritySeverity = SecuritySeverity.INFO,
)

enum class SecuritySeverity { INFO, WARNING, ALERT }

/** Privacy data entry. */
data class PrivacyDataEntry(
    val label: String,
    val description: String,
    val size: String,
)

/** State for [SecurityScreen]. */
data class SecurityScreenState(
    val isArmed: Boolean = false,
    val antiTheftToggles: List<AntiTheftToggle> = emptyList(),
    val permissionGroups: List<PermissionGroup> = emptyList(),
    val permissionGrantedMap: Map<String, Boolean> = emptyMap(),
    val activityLog: List<SecurityLogEntry> = emptyList(),
    val privacyData: List<PrivacyDataEntry> = emptyList(),
)

// ── Screen ─────────────────────────────────────────────────────────────

/**
 * Security & anti-theft screen.
 *
 * Shows armed/disarmed status, anti-theft toggles, permission center,
 * activity history, privacy dashboard, and SIM lock info.
 *
 * @param state Current security state.
 * @param onToggleArm Toggle armed/disarmed.
 * @param onAntiTheftToggle Called with the label and new enabled state.
 * @param onRequestPermission Request a specific permission.
 * @param onClearData Clear stored privacy data.
 */
@Composable
fun SecurityScreen(
    state: SecurityScreenState = remember { SecurityScreenState() },
    onToggleArm: () -> Unit = {},
    onAntiTheftToggle: (String, Boolean) -> Unit = { _, _ -> },
    onRequestPermission: (String) -> Unit = {},
    onClearData: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TarzoDark)
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        // ── Header ────────────────────────────────────────────────
        item {
            Text(
                text = "Security",
                color = TarzoTextPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Protect your device and data",
                color = TarzoTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // ── Security status card ─────────────────────────────────
        item {
            SecurityStatusCard(
                isArmed = state.isArmed,
                onToggle = onToggleArm,
            )
        }

        // ── Anti-theft toggles ────────────────────────────────────
        item {
            SectionTitle(title = "Anti-Theft", icon = Icons.Default.Shield)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(
            items = state.antiTheftToggles,
            key = { it.label },
        ) { toggle ->
            AntiTheftToggleCard(
                toggle = toggle,
                onToggle = { onAntiTheftToggle(toggle.label, !toggle.isEnabled) },
            )
        }

        // ── Permission center ────────────────────────────────────
        item {
            SectionTitle(title = "Permission Center", icon = Icons.Default.AdminPanelSettings)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.permissionGroups.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = TarzoCard),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No permission groups configured",
                            color = TarzoTextSecondary.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        } else {
            items(
                items = state.permissionGroups,
                key = { it.name },
            ) { group ->
                val isGranted = state.permissionGrantedMap[group.name] ?: false
                PermissionCard(
                    entry = PermissionEntry(
                        name = group.name,
                        description = group.rationale,
                        status = if (isGranted) PermissionStatus.GRANTED
                        else PermissionStatus.NOT_ASKED,
                        icon = permissionIcon(group.name),
                        onRequest = { onRequestPermission(group.name) },
                    ),
                )
            }
        }

        // ── Activity history ──────────────────────────────────────
        item {
            SectionTitle(title = "Activity History", icon = Icons.Default.History)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.activityLog.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No activity recorded",
                        color = TarzoTextSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            items(
                items = state.activityLog,
                key = { it.id },
            ) { entry ->
                ActivityLogRow(entry = entry)
            }
        }

        // ── Privacy dashboard ─────────────────────────────────────
        item {
            SectionTitle(title = "Privacy Dashboard", icon = Icons.Default.PrivacyTip)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = TarzoCard),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.privacyData.forEach { entry ->
                        PrivacyDataRow(entry = entry)
                    }
                    HorizontalDivider(color = TarzoDivider)
                    OutlinedButton(
                        onClick = onClearData,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TarzoError.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TarzoError),
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Data", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // ── SIM lock info ─────────────────────────────────────────
        item {
            SectionTitle(title = "SIM Lock", icon = Icons.Default.SimCard)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = TarzoCard),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SimInfoRow(label = "Status", value = "Not Configured", color = TarzoWarning)
                    SimInfoRow(label = "Carrier", value = "--")
                    SimInfoRow(label = "Network", value = "--")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ── Security status card ───────────────────────────────────────────────

@Composable
private fun SecurityStatusCard(
    isArmed: Boolean,
    onToggle: () -> Unit,
) {
    val statusColor by animateColorAsState(
        targetValue = if (isArmed) TarzoSuccess else TarzoError,
        animationSpec = tween(400),
        label = "sec_status_color",
    )
    val cardBg by animateColorAsState(
        targetValue = if (isArmed) TarzoSuccess.copy(alpha = 0.08f) else TarzoError.copy(alpha = 0.08f),
        animationSpec = tween(400),
        label = "sec_card_bg",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = cardBg),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isArmed) Icons.Default.Security else Icons.Default.NoEncryption,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArmed) "Armed" else "Disarmed",
                    color = statusColor,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = if (isArmed) "Anti-theft protection is active" else "Tap to enable protection",
                    color = TarzoTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = isArmed,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = statusColor,
                    checkedTrackColor = statusColor.copy(alpha = 0.3f),
                ),
            )
        }
    }
}

// ── Anti-theft toggle card ─────────────────────────────────────────────

@Composable
private fun AntiTheftToggleCard(
    toggle: AntiTheftToggle,
    onToggle: () -> Unit,
) {
    val activeColor = if (toggle.isEnabled) TarzoAccent else TarzoTextSecondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = TarzoCard),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = toggle.icon,
                contentDescription = null,
                tint = activeColor,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = toggle.label,
                    color = TarzoTextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = toggle.description,
                    color = TarzoTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = toggle.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TarzoAccent,
                    checkedTrackColor = TarzoAccent.copy(alpha = 0.3f),
                ),
            )
        }
    }
}

// ── Activity log row ───────────────────────────────────────────────────

@Composable
private fun ActivityLogRow(entry: SecurityLogEntry) {
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val severityColor = when (entry.severity) {
        SecuritySeverity.INFO -> TarzoTextSecondary
        SecuritySeverity.WARNING -> TarzoWarning
        SecuritySeverity.ALERT -> TarzoError
    }
    val severityIcon: ImageVector = when (entry.severity) {
        SecuritySeverity.INFO -> Icons.Default.Info
        SecuritySeverity.WARNING -> Icons.Default.Warning
        SecuritySeverity.ALERT -> Icons.Default.Error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = severityIcon,
            contentDescription = null,
            tint = severityColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.event,
                color = TarzoTextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.detail.isNotBlank()) {
                Text(
                    text = entry.detail,
                    color = TarzoTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = sdf.format(Date(entry.timestamp)),
            color = TarzoTextSecondary.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

// ── Privacy data row ───────────────────────────────────────────────────

@Composable
private fun PrivacyDataRow(entry: PrivacyDataEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.label,
            color = TarzoTextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = entry.size,
            color = TarzoTextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

// ── SIM info row ───────────────────────────────────────────────────────

@Composable
private fun SimInfoRow(
    label: String,
    value: String,
    color: Color = TarzoTextSecondary,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = TarzoTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TarzoAccent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = TarzoTextPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun permissionIcon(name: String): ImageVector {
    return when (name.lowercase()) {
        "microphone" -> Icons.Default.Mic
        "camera" -> Icons.Default.CameraAlt
        "phone" -> Icons.Default.Phone
        "sms" -> Icons.Default.Sms
        "storage" -> Icons.Default.Storage
        "notifications" -> Icons.Default.Notifications
        "bluetooth" -> Icons.Default.Bluetooth
        "overlay" -> Icons.Default.Layers
        "alarm" -> Icons.Default.Alarm
        else -> Icons.Default.Shield
    }
}