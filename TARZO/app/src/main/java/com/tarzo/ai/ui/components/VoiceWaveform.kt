package com.tarzo.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tarzo.ai.ui.theme.TarzoAccent
import com.tarzo.ai.ui.theme.TarzoAccentSecondary
import kotlin.math.sin

/**
 * Animated voice waveform visualization.
 *
 * Renders a series of vertical bars whose heights oscillate when
 * [isListening] is true. When idle the bars collapse to a small
 * minimum height with a subtle idle animation.
 *
 * @param isListening True when the speech recognizer is actively
 *   capturing audio.
 * @param amplitudeLevel Optional 0-1 value from the audio pipeline.
 *   When null the component generates a synthetic wave pattern.
 * @param modifier Modifier applied to the outer box.
 * @param barCount Number of vertical bars to render.
 * @param barColor Color used for the bars. Defaults to [TarzoAccent].
 * @param inactiveColor Color used when not listening.
 */
@Composable
fun VoiceWaveform(
    isListening: Boolean,
    modifier: Modifier = Modifier,
    amplitudeLevel: Float? = null,
    barCount: Int = 40,
    barColor: Color = TarzoAccent,
    inactiveColor: Color = TarzoAccent.copy(alpha = 0.25f),
) {
    // Infinite transition for continuous animation.
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    // Per-bar animated phases.
    val phases = remember {
        List(barCount) { index ->
            // Stagger each bar's phase offset for a natural wave.
            (index * 0.15f) % (2f * kotlin.math.PI.toFloat())
        }
    }

    // Master time value drives the wave motion.
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave_time",
    )

    // Smooth transition between listening / idle.
    val listeningFraction by animateFloatAsState(
        targetValue = if (isListening) 1f else 0f,
        animationSpec = tween(300),
        label = "listening_frac",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            val gap = 2.dp.toPx()
            val barWidth = (totalWidth - gap * (barCount - 1)) / barCount
            val maxBarHeight = totalHeight * 0.85f
            val minBarHeight = totalHeight * 0.08f

            for (i in 0 until barCount) {
                val x = i * (barWidth + gap)

                // Compute bar height.
                val rawHeight = if (isListening || listeningFraction > 0.01f) {
                    val phase = phases[i]
                    // Combine sine waves for organic motion.
                    val wave1 = sin(phase + time * 2f * kotlin.math.PI.toFloat()).toFloat()
                    val wave2 = sin(phase * 1.7f + time * 3f * kotlin.math.PI.toFloat()).toFloat()
                    val combined = (wave1 * 0.6f + wave2 * 0.4f + 1f) / 2f // normalize 0..1

                    // Apply external amplitude if provided, otherwise use synthetic.
                    val amp = amplitudeLevel ?: combined
                    val height = minBarHeight + (maxBarHeight - minBarHeight) * amp * listeningFraction
                    height
                } else {
                    // Idle: very subtle breathing.
                    val idlePulse = sin(phases[i] + time * kotlin.math.PI.toFloat()).toFloat()
                    minBarHeight + (minBarHeight * 0.5f) * (idlePulse * 0.5f + 0.5f)
                }

                val barHeight = rawHeight.coerceAtLeast(minBarHeight)
                val y = (totalHeight - barHeight) / 2f

                // Gradient from accent to secondary across the bars.
                val fraction = i / (barCount - 1f)
                val color = if (listeningFraction > 0.01f) {
                    lerp(inactiveColor, barColor, fraction)
                } else {
                    inactiveColor
                }

                drawRoundRect(
                    color = color.copy(alpha = if (listeningFraction > 0.01f) 0.9f else 0.4f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
            }
        }
    }
}

/** Linear interpolation between two colours. */
private fun lerp(a: Color, b: Color, t: Float): Color {
    return Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = a.alpha + (b.alpha - a.alpha) * t,
    )
}
