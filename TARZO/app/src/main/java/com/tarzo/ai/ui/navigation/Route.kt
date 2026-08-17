package com.tarzo.ai.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes for the TARZO app.
 *
 * Each route carries a stable [route] string used by
 * `NavHost` and a human-readable [label] for display in the
 * bottom navigation bar.
 *
 * Optional arguments are encoded as query parameters in
 * the route string and exposed as typed properties for
 * convenient use in composables.
 */
sealed class Route(val route: String, val label: String, val icon: ImageVector) {

    /** Main voice-assistant screen with the orb, waveform, and conversation. */
    data object Home : Route("home", "Home", Icons.Default.Home)

    /** Dedicated voice interaction / conversation screen. */
    data object Voice : Route("voice", "Voice", Icons.Default.Mic)

    /** Device automation routines and rules. */
    data object Automation : Route("automation", "Automate", Icons.Default.AutoMode)

    /** Vision / camera / OCR screen. */
    data object Vision : Route("vision", "Vision", Icons.Default.CameraAlt)

    /** Persistent memory browser and search. */
    data object Memory : Route("memory", "Memory", Icons.Default.Memory)

    /** Security / anti-theft settings. */
    data object Security : Route("security", "Security", Icons.Default.Security)

    /** App settings (language, voice, wake word, etc.). */
    data object Settings : Route("settings", "Settings", Icons.Default.Settings)

    companion object {
        /** All routes that appear in the bottom navigation bar. */
        val bottomNavItems = listOf(Home, Voice, Automation, Vision, Memory)
    }
}
