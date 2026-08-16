package com.tarzo.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * The main TARZO theme composable.
 *
 * Defaults to dark theme (matching the futuristic aesthetic). Falls back to
 * the system dark/light setting only when [forceDark] is explicitly false.
 *
 * @param forceDark When true (default) the dark scheme is always used
 *   regardless of the system setting. Set to false to respect the
 *   system dark-mode toggle.
 * @param content The composable content tree.
 */
@Composable
fun TarzoTheme(
    forceDark: Boolean = true,
    content: @Composable () -> Unit,
) {
    val useDark = forceDark || isSystemInDarkTheme()
    val colorScheme = if (useDark) TarzoDarkColorScheme else TarzoLightColorScheme

    // Ensure the status-bar and navigation-bar are transparent / tinted.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TarzoTypography,
        shapes = TarzoShapes,
        content = content,
    )
}

/**
 * Custom shape definitions used throughout TARZO.
 *
 * Small rounding for chips / badges, medium for cards / dialogs,
 * large for bottom sheets and full-width containers.
 */
val TarzoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
