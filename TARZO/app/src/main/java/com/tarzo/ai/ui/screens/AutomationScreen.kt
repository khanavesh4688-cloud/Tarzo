package com.tarzo.ai.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tarzo.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ── Data models ─────────────────────────────────────────────────────────

/** Represents an app available for quick-launch. */
data class AppEntry(
    val name: String,
    val packageName: String,
    val icon: ImageVector,
)

/** A single automation log entry. */
data class AutomationLogEntry(
    val id: Long,
    val action: String,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
)

/** State for [AutomationScreen]. */
data class AutomationScreenState(
    val isFlashlightOn: Boolean = false,
    val brightness: Float = 0.5f,
    val volume: Float = 0.5f,
    val isWifiOn: Boolean = true,
    val isBluetoothOn: Boolean = false,
    val automationLog: List<AutomationLogEntry> = emptyList(),
)

// ── Screen ─────────────────────────────────────────────────────────────

/**
 * Device automation & control screen.
 *
 * Contains an app-launcher grid, device control cards (flashlight,
 * brightness, volume, WiFi, Bluetooth), screen automation buttons,
 * and an automation history log.
 *
 * @param state Current automation state.
 * @param onToggleFlashlight Called to toggle the flashlight.
 * @param onBrightnessChange Called with the new 0–1 brightness value.
 * @param onVolumeChange Called with the new 0–1 volume value.
 * @param onToggleWifi Called to toggle WiFi.
 * @param onToggleBluetooth Called to toggle Bluetooth.
 * @param onLaunchApp Called with the app package name.
 * @param onScrollUp Simulate scroll up on the active screen.
 * @param onScrollDown Simulate scroll down on the active screen.
 * @param onGetScreenText Read text content from the active screen.
 */
@Composable
fun AutomationScreen(
    state: AutomationScreenState = remember { AutomationScreenState() },
    onToggleFlashlight: () -> Unit = {},
    onBrightnessChange: (Float) -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    onToggleWifi: () -> Unit = {},
    onToggleBluetooth: () -> Unit = {},
    onLaunchApp: (String) -> Unit = {},
    onScrollUp: () -> Unit = {},
    onScrollDown: () -> Unit = {},
    onGetScreenText: () -> Unit = {},
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
                text = "Automation",
                color = TarzoTextPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Control your device with voice or touch",
                color = TarzoTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // ── App Launcher Grid ─────────────────────────────────────
        item {
            SectionHeader(
                title = "Quick Launch",
                icon = Icons.Default.Apps,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppLauncherGrid(
                onAppClick = onLaunchApp,
            )
        }

        // ── Device Controls ───────────────────────────────────────
        item {
            SectionHeader(
                title = "Device Controls",
                icon = Icons.Default.Tune,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            DeviceControlCard(
                icon = if (state.isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                label = "Flashlight",
                isActive = state.isFlashlightOn,
                activeColor = TarzoWarning,
                onClick = onToggleFlashlight,
            )
        }

        item {
            SliderControlCard(
                icon = Icons.Default.BrightnessHigh,
                label = "Brightness",
                value = state.brightness,
                onValueChange = onBrightnessChange,
                activeColor = TarzoYellow,
            )
        }

        item {
            SliderControlCard(
                icon = Icons.Default.VolumeUp,
                label = "Volume",
                value = state.volume,
                onValueChange = onVolumeChange,
                activeColor = TarzoOrange,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DeviceControlCard(
                    icon = if (state.isWifiOn) Icons.Default.Wifi else Icons.Default.WifiOff,
                    label = "WiFi",
                    isActive = state.isWifiOn,
                    activeColor = TarzoSuccess,
                    onClick = onToggleWifi,
                    modifier = Modifier.weight(1f),
                )
                DeviceControlCard(
                    icon = if (state.isBluetoothOn) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                    label = "Bluetooth",
                    isActive = state.isBluetoothOn,
                    activeColor = TarzoBlue,
                    onClick = onToggleBluetooth,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Screen Automation ─────────────────────────────────────
        item {
            SectionHeader(
                title = "Screen Automation",
                icon = Icons.Default.PhoneAndroid,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = TarzoCard),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AutomationActionButton(
                            icon = Icons.Default.KeyboardArrowUp,
                            label = "Scroll Up",
                            onClick = onScrollUp,
                            modifier = Modifier.weight(1f),
                        )
                        AutomationActionButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            label = "Scroll Down",
                            onClick = onScrollDown,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    AutomationActionButton(
                        icon = Icons.Default.TextFields,
                        label = "Get Screen Text",
                        onClick = onGetScreenText,
                        fullWidth = true,
                    )
                }
            }
        }

        // ── Automation History ────────────────────────────────────
        item {
            SectionHeader(
                title = "Recent Actions",
                icon = Icons.Default.History,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.automationLog.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No automation actions yet",
                        color = TarzoTextSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            items(
                items = state.automationLog,
                key = { it.id },
            ) { entry ->
                AutomationLogItem(entry = entry)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ── Section header ─────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
) {
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

// ── App launcher grid ──────────────────────────────────────────────────

@Composable
private fun AppLauncherGrid(
    onAppClick: (String) -> Unit,
) {
    val apps = listOf(
        AppEntry("YouTube", "com.google.android.youtube", Icons.Default.PlayCircle),
        AppEntry("WhatsApp", "com.whatsapp", Icons.Default.Chat),
        AppEntry("Camera", "com.android.camera", Icons.Default.CameraAlt),
        AppEntry("Settings", "com.android.settings", Icons.Default.Settings),
        AppEntry("Chrome", "com.android.chrome", Icons.Default.Public),
        AppEntry("Spotify", "com.spotify.music", Icons.Default.LibraryMusic),
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(220.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = apps) { app ->
            AppGridItem(app = app, onClick = { onAppClick(app.packageName) })
        }
    }
}

@Composable
private fun AppGridItem(
    app: AppEntry,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TarzoCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TarzoSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = app.icon,
                contentDescription = app.name,
                tint = TarzoAccent,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = app.name,
            color = TarzoTextPrimary,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Device control card ────────────────────────────────────────────────

@Composable
private fun DeviceControlCard(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
 val cardColor by animateColorAsState(
        targetValue = if (isActive) activeColor.copy(alpha = 0.15f) else TarzoCard,
        animationSpec = tween(300),
        label = "control_card_color",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = if (isActive) 0.2f else 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) activeColor else TarzoTextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = if (isActive) TarzoTextPrimary else TarzoTextSecondary,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isActive,
                onCheckedChange = { onClick() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = activeColor,
                    checkedTrackColor = activeColor.copy(alpha = 0.3f),
                ),
            )
        }
    }
}

// ── Slider control card ────────────────────────────────────────────────

@Composable
private fun SliderControlCard(
    icon: ImageVector,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    activeColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = TarzoCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = activeColor,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = TarzoTextPrimary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.width(80.dp),
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                    thumbColor = activeColor,
                    activeTrackColor = activeColor,
                    inactiveTrackColor = TarzoSurface,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${(value * 100).toInt()}%",
                color = TarzoTextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

// ── Automation action button ───────────────────────────────────────────

@Composable
private fun AutomationActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false,
) {
    Surface(
        modifier = if (fullWidth) modifier.fillMaxWidth() else modifier,
        shape = RoundedCornerShape(12.dp),
        color = TarzoSurface,
    ) {
        Row(
            modifier = Modifier
                .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (fullWidth) Arrangement.Start else Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TarzoAccent,
                modifier = Modifier.size(20.dp),
            )
            if (fullWidth) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    color = TarzoTextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// ── Automation log item ────────────────────────────────────────────────

@Composable
private fun AutomationLogItem(entry: AutomationLogEntry) {
    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (entry.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (entry.isSuccess) TarzoSuccess else TarzoError,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.action,
                color = TarzoTextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.detail,
                color = TarzoTextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = sdf.format(Date(entry.timestamp)),
            color = TarzoTextSecondary.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
