package com.tarzo.ai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import com.tarzo.ai.ui.theme.*

enum class OrbState {
    IDLE, LISTENING, PROCESSING, SPEAKING, ERROR
}

@Composable
fun TarzoOrb(
    state: OrbState = OrbState.IDLE,
    modifier: Modifier = Modifier,
    size: Int = 200,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val isIdle = state == OrbState.IDLE
    val scale = if (isIdle) 1f else pulseScale

    val orbColor = when (state) {
        OrbState.IDLE -> TarzoAccent.copy(alpha = 0.4f)
        OrbState.LISTENING -> TarzoAccent
        OrbState.PROCESSING -> TarzoAccentSecondary
        OrbState.SPEAKING -> TarzoSuccess
        OrbState.ERROR -> TarzoError
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .scale(scale)
            .clip(CircleShape)
        .background(Brush.radialGradient(listOf(orbColor, TarzoDark)))
        .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "TARZO",
            tint = TarzoTextPrimary.copy(alpha = if (isIdle) 0.5f else 0.9f),
            modifier = Modifier.size((size * 0.35).dp)
        )
    }
}
