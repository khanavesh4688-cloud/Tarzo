package com.tarzo.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tarzo.ai.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visual states for the TARZO orb animation.
 *
 * Maps to the app's voice pipeline states so the UI always reflects
 * what TARZO is doing.
 */
enum class OrbState {
    /** Idle / waiting — gentle breathing pulse. */
    IDLE,
    /** Listening to the user — expanding rings, brighter glow. */
    LISTENING,
    /** Processing / thinking — spinning, pulsing. */
    THINKING,
    /** Speaking a response — waveform-like deformation. */
    SPEAKING,
}

/**
 * The central TARZO orb / avatar.
 *
 * A Canvas-drawn glowing sphere with cyan→purple gradient that animates
 * differently depending on [state]. This is the hero UI element of the app.
 *
 * @param state Current orb state — drives the animation.
 * @param modifier Modifier applied to the outer container.
 * @param size Diameter of the orb (default 180 dp).
 */
@Composable
fun TarzoOrb(
    state: OrbState = OrbState.IDLE,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
) {
    // ── Animations ──────────────────────────────────────────────────

    // Gentle breathing pulse (always active, amplitude varies by state)
    val pulseDuration = when (state) {
        OrbState.IDLE -> 3000f
        OrbState.LISTENING -> 1500f
        OrbState.THINKING -> 800f
        OrbState.SPEAKING -> 600f
    }
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration.toInt(), easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    // Glow radius expansion (used by LISTENING)
    val ringExpansion by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring",
    )

    // Rotation angle (used by THINKING)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    // Wave amplitude (used by SPEAKING)
    val wave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wave",
    )

    // Animated glow alpha
    val glowAlpha by animateFloatAsState(
        targetValue = when (state) {
            OrbState.IDLE -> 0.25f
            OrbState.LISTENING -> 0.6f
            OrbState.THINKING -> 0.45f
            OrbState.SPEAKING -> 0.55f
        },
        animationSpec = tween(400),
        label = "glow_alpha",
    )

    // Animated orb scale
    val orbScale by animateFloatAsState(
        targetValue = when (state) {
            OrbState.IDLE -> 1f
            OrbState.LISTENING -> 1.12f
            OrbState.THINKING -> 1.05f
            OrbState.SPEAKING -> 1.08f
        },
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
        label = "orb_scale",
    )

    // Secondary ring count for LISTENING
    val ringCount = 3

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = (size.minDimension / 2f) * 0.42f * orbScale

            // ── 1. Outer glow ───────────────────────────────────
            val glowRadius = baseRadius * (1.6f + pulse * 0.4f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        TarzoAccent.copy(alpha = glowAlpha * 0.5f),
                        TarzoAccentSecondary.copy(alpha = glowAlpha * 0.3f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = glowRadius,
                ),
                center = Offset(cx, cy),
                radius = glowRadius,
            )

            // ── 2. Expanding rings (LISTENING) ─────────────────
            if (state == OrbState.LISTENING) {
                for (i in 0 until ringCount) {
                    val phase = (ringExpansion + i / ringCount.toFloat()) % 1f
                    val ringRadius = baseRadius * (1f + phase * 1.2f)
                    val ringAlpha = (1f - phase) * 0.4f
                    drawCircle(
                        color = TarzoAccent.copy(alpha = ringAlpha),
                        center = Offset(cx, cy),
                        radius = ringRadius,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            // ── 3. Spinning arcs (THINKING) ────────────────────
            if (state == OrbState.THINKING) {
                val arcCount = 4
                for (i in 0 until arcCount) {
                    val angle = rotation + (i * 360f / arcCount)
                    val startAngle = angle
                    val sweep = 60f + pulse * 30f
                    drawArc(
                        color = TarzoAccent.copy(alpha = 0.35f),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(cx - baseRadius * 1.15f, cy - baseRadius * 1.15f),
                        size = Size(baseRadius * 2.3f, baseRadius * 2.3f),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            // ── 4. Waveform deformation (SPEAKING) ─────────────
            if (state == OrbState.SPEAKING) {
                val barCount = 24
                for (i in 0 until barCount) {
                    val angleDeg = i * 360f / barCount
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val amplitude = baseRadius * 0.15f * (
                        sin((angleDeg + wave * 360f) * Math.PI / 180f).toFloat() * 0.7f + 0.3f
                    )
                    val innerR = baseRadius * 0.95f
                    val outerR = baseRadius * 1.25f + amplitude
                    val x1 = cx + innerR * cos(angleRad).toFloat()
                    val y1 = cy + innerR * sin(angleRad).toFloat()
                    val x2 = cx + outerR * cos(angleRad).toFloat()
                    val y2 = cy + outerR * sin(angleRad).toFloat()
                    drawLine(
                        color = TarzoAccent.copy(alpha = 0.5f + wave * 0.3f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            }

            // ── 5. Main orb sphere ─────────────────────────────
            val gradient = Brush.radialGradient(
                colors = listOf(
                    TarzoOrbCore.copy(alpha = 0.9f + pulse * 0.1f),
                    TarzoOrbGradientStart.copy(alpha = 0.85f),
                    TarzoAccent.copy(alpha = 0.7f),
                    TarzoAccentSecondary.copy(alpha = 0.6f),
                ),
                center = Offset(cx - baseRadius * 0.25f, cy - baseRadius * 0.25f),
                radius = baseRadius * 1.1f,
            )
            drawCircle(
                brush = gradient,
                center = Offset(cx, cy),
                radius = baseRadius,
            )

            // ── 6. Specular highlight ──────────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = Offset(cx - baseRadius * 0.3f, cy - baseRadius * 0.3f),
                    radius = baseRadius * 0.55f,
                ),
                center = Offset(cx - baseRadius * 0.3f, cy - baseRadius * 0.3f),
                radius = baseRadius * 0.55f,
            )

            // ── 7. Thin border ring ────────────────────────────
            drawCircle(
                color = TarzoAccent.copy(alpha = 0.2f + pulse * 0.15f),
                center = Offset(cx, cy),
                radius = baseRadius + 1.dp.toPx(),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

/**
 * Helper: draw an arc on Canvas with separate topLeft / size parameters. */
private fun DrawScope.drawArc(
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    useCenter: Boolean,
    topLeft: Offset,
    size: Size,
    style: androidx.compose.ui.graphics.drawscope.Stroke = Stroke(width = 1f),
) {
    drawArc(
        brush = androidx.compose.ui.graphics.Brush.solid(color),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = useCenter,
        topLeft = topLeft,
        size = size,
        style = style,
    )
}
