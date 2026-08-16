package com.tarzo.ai.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.tarzo.ai.ui.theme.*

// ── State ─────────────────────────────────────────────────────────────

/** State model for [SettingsScreen]. */
data class SettingsScreenState(
    val languageCode: String = "hi-IN",
    val ttsEngine: String = "Google TTS",
    val voicePitch: Float = 1.0f,
    val voiceSpeed: Float = 1.0f,
    val isWakeWordEnabled: Boolean = false,
    val wakeWordSensitivity: Float = 0.65f,
    val apiBaseUrl: String = "",
    val isApiConnected: Boolean = false,
    val isNotificationsEnabled: Boolean = true,
    val isDarkTheme: Boolean = true,
    val appVersion: String = "1.0.0",
)

/** Language option for settings. */
data class SettingsLanguageOption(
    val code: String,
    val label: String,
)

// ── Screen ─────────────────────────────────────────────────────────────

/**
 * App settings screen.
 *
 * Covers language, voice (TTS engine, pitch, speed), wake word,
 * AI model / API configuration, notifications, about, theme toggle,
 * and privacy link.
 *
 * @param state Current settings state.
 * @param onLanguageChange Called with the new language code.
 * @param onTtsEngineChange Called with the new TTS engine name.
 * @param onPitchChange Called with the new pitch value (0.1f – 2.0f).
 * @param onSpeedChange Called with the new speed value (0.1f – 3.0f).
 * @param onWakeWordToggle Called with the new enabled state.
 * @param onWakeWordSensitivityChange Called with the new sensitivity (0.0f – 1.0f).
 * @param onApiBaseUrlChange Called with the new base URL string.
 * @param onTestConnection Trigger an API connectivity test.
 * @param onNotificationToggle Called with the new enabled state.
 * @param onThemeToggle Called with the new dark-mode preference.
 * @param onNavigateToPrivacy Navigate to the privacy/security screen.
 */
@Composable
fun SettingsScreen(
    state: SettingsScreenState = remember { SettingsScreenState() },
    onLanguageChange: (String) -> Unit = {},
    onTtsEngineChange: (String) -> Unit = {},
    onPitchChange: (Float) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onWakeWordToggle: (Boolean) -> Unit = {},
    onWakeWordSensitivityChange: (Float) -> Unit = {},
    onApiBaseUrlChange: (String) -> Unit = {},
    onTestConnection: () -> Unit = {},
    onNotificationToggle: (Boolean) -> Unit = {},
    onThemeToggle: (Boolean) -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TarzoDark)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Settings",
            color = TarzoTextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Customize your TARZO experience",
            color = TarzoTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Language preference ───────────────────────────────────
        SettingsSectionHeader(title = "Language", icon = Icons.Default.Language)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsLanguageSelector(
            currentCode = state.languageCode,
            onSelected = onLanguageChange,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Voice settings ────────────────────────────────────────
        SettingsSectionHeader(title = "Voice", icon = Icons.Default.RecordVoiceOver)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = TarzoCard),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // TTS Engine.
                SettingsDropdownRow(
                    label = "TTS Engine",
                    value = state.ttsEngine,
                    options = listOf("Google TTS", "System TTS"),
                    onSelected = onTtsEngineChange,
                )
                HorizontalDivider(color = TarzoDivider)
                // Pitch.
                Text(
                    text = "Pitch: ${"%.1f".format(state.voicePitch)}",
                    color = TarzoTextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = state.voicePitch,
                    onValueChange = onPitchChange,
                    valueRange = 0.1f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = TarzoAccent,
                        activeTrackColor = TarzoAccent,
                        inactiveTrackColor = TarzoSurface,
                    ),
                )
                // Speed.
                Text(
                    text = "Speed: ${"%.1f".format(state.voiceSpeed)}",
                    color = TarzoTextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = state.voiceSpeed,
                    onValueChange = onSpeedChange,
                    valueRange = 0.1f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = TarzoAccent,
                        activeTrackColor = TarzoAccent,
                        inactiveTrackColor = TarzoSurface,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Wake word settings ────────────────────────────────────
        SettingsSectionHeader(title = "Wake Word", icon = Icons.Default.GraphicEq)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = TarzoCard),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Wake Word",
                            color = TarzoTextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "\"Bolo TARZO\" to activate",
                            color = TarzoTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = state.isWakeWordEnabled,
                        onCheckedChange = onWakeWordToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TarzoAccent,
                            checkedTrackColor = TarzoAccent.copy(alpha = 0.3f),
                        ),
                    )
                }
                if (state.isWakeWordEnabled) {
                    HorizontalDivider(color = TarzoDivider)
                    Text(
                        text = "Sensitivity: ${"%.0f".format(state.wakeWordSensitivity * 100)}%",
                        color = TarzoTextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Slider(
                        value = state.wakeWordSensitivity,
                        onValueChange = onWakeWordSensitivityChange,
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = TarzoAccentSecondary,
                            activeTrackColor = TarzoAccentSecondary,
                            inactiveTrackColor = TarzoSurface,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── AI Model / API ────────────────────────────────────────
        SettingsSectionHeader(title = "AI Model", icon = Icons.Default.Psychology)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = TarzoCard),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.apiBaseUrl,
                    onValueChange = onApiBaseUrlChange,
                    label = { Text("API Base URL") },
                    placeholder = { Text("https://api.example.com", color = TarzoTextSecondary.copy(alpha = 0.4f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TarzoTextPrimary,
                        unfocusedTextColor = TarzoTextPrimary,
                        cursorColor = TarzoAccent,
                        focusedBorderColor = TarzoAccent,
                        unfocusedBorderColor = TarzoDivider,
                        focusedContainerColor = TarzoSurface,
                        unfocusedContainerColor = TarzoSurface,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(
                        onClick = onTestConnection,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = TarzoAccent.copy(alpha = 0.15f),
                            contentColor = TarzoAccent,
                        ),
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Connection", style = MaterialTheme.typography.labelLarge)
                    }
                    if (state.isApiConnected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            tint = TarzoSuccess,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Notification settings ─────────────────────────────────
        SettingsSectionHeader(title = "Notifications", icon = Icons.Default.Notifications)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsToggleRow(
            label = "Enable Notifications",
            description = "Receive alerts for reminders and security events",
            checked = state.isNotificationsEnabled,
            onCheckedChange = onNotificationToggle,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Theme toggle ──────────────────────────────────────────
        SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Palette)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsToggleRow(
            label = "Dark Theme",
            description = "Use dark colour scheme throughout the app",
            checked = state.isDarkTheme,
            onCheckedChange = onThemeToggle,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Privacy link ──────────────────────────────────────────
        SettingsSectionHeader(title = "Privacy & Security", icon = Icons.Default.PrivacyTip)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsNavigationRow(
            label = "Open Privacy Settings",
            icon = Icons.Default.Security,
            onClick = onNavigateToPrivacy,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── About ─────────────────────────────────────────────────
        SettingsSectionHeader(title = "About", icon = Icons.Default.Info)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = TarzoCard),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingsInfoRow(label = "App", value = "TARZO AI Assistant")
                SettingsInfoRow(label = "Version", value = state.appVersion)
                SettingsInfoRow(label = "Build", value = "2025.06.01")
                HorizontalDivider(color = TarzoDivider)
                Text(
                    text = "Built with passion for a smarter mobile experience.",
                    color = TarzoTextSecondary.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Section header ─────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(
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

// ── Language selector ──────────────────────────────────────────────────

@Composable
private fun SettingsLanguageSelector(
    currentCode: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        SettingsLanguageOption("hi-IN", "Hindi"),
        SettingsLanguageOption("en-IN", "English (India)"),
        SettingsLanguageOption("en-US", "English (US)"),
    )
    val current = options.find { it.code == currentCode } ?: options.first()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = TarzoCard),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    tint = TarzoAccent,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Language",
                        color = TarzoTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = current.label,
                        color = TarzoTextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TarzoTextSecondary)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = TarzoSurface,
                shape = RoundedCornerShape(12.dp),
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opt.label,
                                color = if (opt.code == currentCode) TarzoAccent else TarzoTextPrimary,
                            )
                        },
                        onClick = {
                            onSelected(opt.code)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

// ── Dropdown row ───────────────────────────────────────────────────────

@Composable
private fun SettingsDropdownRow(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = TarzoTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(90.dp),
        )
        Box {
            Surface(
                modifier = Modifier.clickable { expanded = true },
                shape = RoundedCornerShape(8.dp),
                color = TarzoSurface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = value,
                        color = TarzoTextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TarzoTextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = TarzoSurface,
                shape = RoundedCornerShape(10.dp),
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opt,
                                color = if (opt == value) TarzoAccent else TarzoTextPrimary,
                            )
                        },
                        onClick = {
                            onSelected(opt)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

// ── Toggle row ─────────────────────────────────────────────────────────

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = TarzoCard),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = TarzoTextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = description,
                    color = TarzoTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TarzoAccent,
                    checkedTrackColor = TarzoAccent.copy(alpha = 0.3f),
                ),
            )
        }
    }
}

// ── Navigation row ─────────────────────────────────────────────────────

@Composable
private fun SettingsNavigationRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = TarzoCard),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = TarzoAccent, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = TarzoTextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TarzoTextSecondary)
        }
    }
}

// ── Info row ───────────────────────────────────────────────────────────

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
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
            color = TarzoTextPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}