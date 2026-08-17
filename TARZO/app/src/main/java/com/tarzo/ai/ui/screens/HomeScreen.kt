package com.tarzo.ai.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tarzo.ai.ui.components.*
import com.tarzo.ai.ui.navigation.Route
import com.tarzo.ai.ui.theme.*

/**
 * UI state model for the home screen.
 *
 * @param orbState Current animated state of the orb.
 * @param statusLabel Floating label shown above the orb (e.g. "Listening...").
 * @param isListening Whether the microphone is actively capturing audio.
 * @param recentCommands Last N voice commands shown as chips.
 * @param conversationMessages Full conversation history rendered as bubbles.
 */
data class HomeScreenState(
    val orbState: OrbState = OrbState.IDLE,
    val statusLabel: String = "",
    val isListening: Boolean = false,
    val recentCommands: List<String> = emptyList(),
    val conversationMessages: List<ConversationMessage> = emptyList(),
)

/**
 * Main home screen of TARZO — the hero screen with the glowing orb,
 * microphone button, waveform, conversation history, and quick controls.
 *
 * Everything is driven by [state] and callbacks — no ViewModel dependency.
 *
 * @param navController Navigation controller for navigating to sub-screens.
 * @param state Current UI state.
 * @param onMicClick Called when the user taps the mic button.
 * @param onQuickAction Called with the action label when a quick-control is tapped.
 */
@Composable
fun HomeScreen(
    navController: NavController,
    state: HomeScreenState = remember { HomeScreenState() },
    onMicClick: () -> Unit = {},
    onQuickAction: (String) -> Unit = {},
) {
    val conversationListState = rememberLazyListState()

    // Auto-scroll conversation to the bottom when new messages arrive.
    LaunchedEffect(state.conversationMessages.size) {
        if (state.conversationMessages.isNotEmpty()) {
            conversationListState.animateScrollToItem(state.conversationMessages.lastIndex)
        }
    }

    // Cycle orb state for demo purposes when no real state is wired.
    val demoOrbStateFloat by rememberInfiniteTransition(label = "orb_cycle").animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orb_demo",
    )
    val demoOrbState = demoOrbStateFloat.toInt().coerceIn(0, 3)
    val effectiveOrbState = remember(state.orbState, demoOrbState) {
        if (state.orbState != OrbState.IDLE) state.orbState
        else listOf(OrbState.IDLE, OrbState.LISTENING, OrbState.PROCESSING, OrbState.SPEAKING)[demoOrbState]
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TarzoDark),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Top bar ───────────────────────────────────────────
            HomeTopBar(navController = navController)

            Spacer(modifier = Modifier.height(4.dp))

            // ── Status indicator ──────────────────────────────────
            if (state.statusLabel.isNotBlank()) {
                StatusIndicator(
                    label = state.statusLabel,
                    orbState = effectiveOrbState,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── Conversation history (scrollable above orb) ──────
            if (state.conversationMessages.isNotEmpty()) {
                LazyColumn(
                    state = conversationListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                    reverseLayout = false,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    items(
                        items = state.conversationMessages,
                        key = { it.id },
                    ) { message ->
                        ConversationBubble(message = message)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Spacer to push orb down ───────────────────────────
            Spacer(modifier = Modifier.weight(1f))

            // ── TARZO Orb ─────────────────────────────────────────
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                TarzoOrb(
                    state = effectiveOrbState,
                    size = 180,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Mic button ────────────────────────────────────────
            MicButton(
                isListening = state.isListening,
                onClick = onMicClick,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Voice waveform ───────────────────────────────────
            VoiceWaveform(
                isListening = state.isListening,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(48.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Recent commands (chips/pills) ─────────────────────
            if (state.recentCommands.isNotEmpty()) {
                Text(
                    text = "Recent",
                    color = TarzoTextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.recentCommands.take(5).forEach { cmd ->
                        CommandChip(
                            text = cmd,
                            onClick = { onQuickAction(cmd) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // ── Quick controls row ───────────────────────────────
            QuickControls(
                actions = listOf(
                    DefaultQuickActions.flashLight(false) { onQuickAction("flashlight") },
                    DefaultQuickActions.camera { navController.navigate(Route.Vision.route) },
                    DefaultQuickActions.wifi(true) { onQuickAction("wifi") },
                    DefaultQuickActions.bluetooth(true) { onQuickAction("bluetooth") },
                    DefaultQuickActions.volume { onQuickAction("volume") },
                    DefaultQuickActions.brightness { onQuickAction("brightness") },
                ),
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "TARZO",
            color = TarzoAccent,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { navController.navigate(Route.Memory.route) }) {
            Icon(
                Icons.Default.Memory,
                contentDescription = "Memory",
                tint = TarzoTextSecondary,
            )
        }
        IconButton(onClick = { navController.navigate(Route.Settings.route) }) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TarzoTextSecondary,
            )
        }
    }
}

// ── Floating status indicator ──────────────────────────────────────────

@Composable
private fun StatusIndicator(
    label: String,
    orbState: OrbState,
) {
    val color by animateColorAsState(
        targetValue = when (orbState) {
            OrbState.IDLE -> TarzoTextSecondary
            OrbState.LISTENING -> TarzoAccent
            OrbState.PROCESSING -> TarzoAccentSecondary
            OrbState.ERROR -> TarzoError
            else -> TarzoTextSecondary
        },
        label = "status_color",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(TarzoCard.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Pulsing dot.
        val pulseAlpha by rememberInfiniteTransition(label = "pulse_dot").animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot_pulse",
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = pulseAlpha)),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

// ── Mic button ─────────────────────────────────────────────────────────

@Composable
private fun MicButton(
    isListening: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 300f),
        label = "mic_scale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isListening) 0.6f else 0.0f,
        animationSpec = tween(300),
        label = "mic_glow",
    )

    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer glow ring.
        if (glowAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(TarzoAccent.copy(alpha = glowAlpha * 0.3f)),
            )
        }
        // Button surface.
        Box(
            modifier = Modifier
                .size(72.dp * scale)
                .clip(CircleShape)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                TarzoAccent.copy(alpha = 0.25f),
                                TarzoAccent.copy(alpha = 0.05f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.width / 2 * 1.3f,
                        ),
                    )
                }
                .background(
                    if (isListening) TarzoAccent else TarzoCard,
                    CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                contentDescription = "Microphone",
                tint = if (isListening) TarzoDark else TarzoAccent,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

// ── Command chip ───────────────────────────────────────────────────────

@Composable
private fun CommandChip(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = TarzoCard,
        contentColor = TarzoTextSecondary,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text,
            color = TarzoTextSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
