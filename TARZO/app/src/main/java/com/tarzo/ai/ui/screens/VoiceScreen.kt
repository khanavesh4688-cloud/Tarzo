package com.tarzo.ai.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tarzo.ai.core.voice.SpeechState
import com.tarzo.ai.core.voice.TTSState
import com.tarzo.ai.ui.components.*
import com.tarzo.ai.ui.theme.*

/**
 * Language option for the voice selector.
 */
data class LanguageOption(
    val code: String,
    val label: String,
)

/**
 * State model for [VoiceScreen].
 */
data class VoiceScreenState(
    val speechState: SpeechState = SpeechState.Idle,
    val ttsState: TTSState = TTSState.IDLE,
    val liveTranscription: String = "",
    val tarzoResponse: String = "",
    val selectedLanguage: LanguageOption = LanguageOption("hi-IN", "Hindi"),
    val isWakeWordEnabled: Boolean = false,
    val voicePitch: Float = 1.0f,
    val voiceSpeed: Float = 1.0f,
    val conversation: List<ConversationMessage> = emptyList(),
)

/**
 * Dedicated voice assistant interaction screen.
 *
 * Shows the orb, live transcription, TARZO response, language selector,
 * wake word toggle, voice settings sliders, and the full conversation history.
 *
 * @param state Drives every visual element on this screen.
 * @param onMicClick Toggle microphone listening.
 * @param onLanguageChange Called with the new language code.
 * @param onWakeWordToggle Called with the new enabled state.
 * @param onPitchChange Called with the new pitch value (0.1f – 2.0f).
 * @param onSpeedChange Called with the new speed value (0.1f – 3.0f).
 * @param onStopSpeaking Stop current TTS playback.
 */
@Composable
fun VoiceScreen(
    state: VoiceScreenState = remember { VoiceScreenState() },
    onMicClick: () -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
    onWakeWordToggle: (Boolean) -> Unit = {},
    onPitchChange: (Float) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onStopSpeaking: () -> Unit = {},
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.conversation.size) {
        if (state.conversation.isNotEmpty()) {
            listState.animateScrollToItem(state.conversation.lastIndex)
        }
    }

    val orbState = when {
        state.speechState is SpeechState.Listening ||
            state.speechState is SpeechState.PartialResult -> OrbState.LISTENING

        state.ttsState == TTSState.SPEAKING -> OrbState.SPEAKING
        state.speechState is SpeechState.FinalResult -> OrbState.PROCESSING
        else -> OrbState.IDLE
    }

    val isListening = state.speechState is SpeechState.Listening ||
            state.speechState is SpeechState.PartialResult

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TarzoDark),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Header ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Voice Assistant",
                    color = TarzoTextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.weight(1f))
                LanguageSelector(
                    selected = state.selectedLanguage,
                    onSelected = { onLanguageChange(it.code) },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Orb ───────────────────────────────────────────────
            TarzoOrb(
                state = orbState,
                size = 160,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Mic button ────────────────────────────────────────
            MicButtonSmall(
                isListening = isListening,
                onClick = onMicClick,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Live transcription ────────────────────────────────
            LiveTranscriptionBox(
                text = state.liveTranscription,
                isListening = isListening,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── TARZO response ────────────────────────────────────
            ResponseBox(
                text = state.tarzoResponse,
                isSpeaking = state.ttsState == TTSState.SPEAKING,
                onStop = onStopSpeaking,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Voice settings section ────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = TarzoCard),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    // Wake word toggle.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = TarzoAccent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wake Word",
                                color = TarzoTextPrimary,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Say \"Bolo TARZO\" to activate",
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

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = TarzoDivider,
                    )

                    // Pitch slider.
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
                            inactiveTrackColor = TarzoCard,
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speed slider.
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
                            inactiveTrackColor = TarzoCard,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Conversation history ──────────────────────────────
            Text(
                text = "Conversation",
                color = TarzoTextSecondary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (state.conversation.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No conversation yet. Tap the mic to start.",
                                color = TarzoTextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    items(
                        items = state.conversation,
                        key = { it.id },
                    ) { message ->
                        ConversationBubble(message = message)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Language selector dropdown ─────────────────────────────────────────

@Composable
private fun LanguageSelector(
    selected: LanguageOption,
    onSelected: (LanguageOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        LanguageOption("hi-IN", "Hindi"),
        LanguageOption("en-IN", "English"),
        LanguageOption("hi-en", "Hinglish"),
    )

    Box {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = TarzoCard,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    tint = TarzoAccent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selected.label,
                    color = TarzoTextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = TarzoTextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
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
                            color = if (opt.code == selected.code) TarzoAccent else TarzoTextPrimary,
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

// ── Mic button (compact) ──────────────────────────────────────────────

@Composable
private fun MicButtonSmall(
    isListening: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "mic_small_scale",
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier.size(60.dp * scale),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isListening) TarzoAccent else TarzoCard)
                .border(2.dp, TarzoAccent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                contentDescription = "Microphone",
                tint = if (isListening) TarzoDark else TarzoAccent,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

// ── Live transcription box ─────────────────────────────────────────────

@Composable
private fun LiveTranscriptionBox(
    text: String,
    isListening: Boolean,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isListening) TarzoAccent else TarzoCard,
        animationSpec = tween(300),
        label = "transcript_border",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = TarzoSurface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = TarzoBlue,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You",
                    color = TarzoTextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (isListening) {
                    Spacer(modifier = Modifier.width(8.dp))
                    PulsingDot(color = TarzoAccent)
                }
            }
            if (text.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    color = TarzoTextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isListening) "Listening..." else "Your speech will appear here",
                    color = TarzoTextSecondary.copy(alpha = if (isListening) 0.6f else 0.4f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// ── TARZO response box ─────────────────────────────────────────────────

@Composable
private fun ResponseBox(
    text: String,
    isSpeaking: Boolean,
    onStop: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = TarzoCard),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = TarzoAccent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TARZO",
                    color = TarzoTextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (isSpeaking) {
                    Spacer(modifier = Modifier.width(8.dp))
                    PulsingDot(color = TarzoSuccess)
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isSpeaking) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop speaking",
                            tint = TarzoError,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (text.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    color = TarzoTextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "TARZO's response will appear here",
                    color = TarzoTextSecondary.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// ── Pulsing dot ─────────────────────────────────────────────────────────

@Composable
private fun PulsingDot(color: Color) {
    val alpha by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot_alpha",
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
        .background(color.copy(alpha = alpha)),
    )
}
