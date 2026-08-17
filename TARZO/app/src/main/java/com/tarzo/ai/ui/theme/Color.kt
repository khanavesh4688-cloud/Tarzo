package com.tarzo.ai.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary Palette ──────────────────────────────────────────────────────

/** Deep navy-black primary background */
val TarzoDark = Color(0xFF0A0E1A)

/** Elevated surface behind cards and sheets */
val TarzoSurface = Color(0xFF111827)

/** Card / container fill */
val TarzoCard = Color(0xFF1E293B)

/** Cyan accent — primary interactive colour */
val TarzoAccent = Color(0xFF00D4FF)

/** Purple accent — secondary highlight */
val TarzoAccentSecondary = Color(0xFF7C3AED)

// ── Text ─────────────────────────────────────────────────────────────────

val TarzoTextPrimary = Color(0xFFF8FAFC)
val TarzoTextSecondary = Color(0xFF94A3B8)

// ── Semantic ─────────────────────────────────────────────────────────────

val TarzoSuccess = Color(0xFF10B981)
val TarzoWarning = Color(0xFFF59E0B)
val TarzoError = Color(0xFFEF4444)

// ── Orb Gradient ─────────────────────────────────────────────────────────

/** Start colour of the orb gradient (cyan) */
val TarzoOrbGradientStart = Color(0xFF00D4FF)

/** End colour of the orb gradient (purple) */
val TarzoOrbGradientEnd = Color(0xFF7C3AED)

/** Outer glow tint behind the orb */
val TarzoOrbGlow = Color(0x3300D4FF)

/** Bright core of the orb during active states */
val TarzoOrbCore = Color(0xB3FFFFFF)

/** Ripple ring colour for listening state */
val TarzoOrbRing = Color(0x4D00D4FF)

// ── Extended palette (for charts, graphs, tags) ─────────────────────────

val TarzoBlue = Color(0xFF3B82F6)
val TarzoIndigo = Color(0xFF6366F1)
val TarzoPink = Color(0xFFEC4899)
val TarzoOrange = Color(0xFFF97316)
val TarzoTeal = Color(0xFF14B8A6)
val TarzoYellow = Color(0xFFEAB308)

// ── Surface overlays ─────────────────────────────────────────────────────

val TarzoSurfaceOverlay = Color(0x0AFFFFFF)   // very subtle white tint
val TarzoScrim = Color(0x80000000)           // dimming scrim
val TarzoDivider = Color(0x1FFFFFFF)         // hairline dividers

// ── Bottom nav / chrome ──────────────────────────────────────────────────

val TarzoNavBackground = Color(0xB3111827)   // surface @ 70 %
val TarzoNavSelected = TarzoAccent
val TarzoNavUnselected = Color(0xFF64748B)

// ── Material3 Color Schemes ─────────────────────────────────────────────

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

val TarzoDarkColorScheme = darkColorScheme(
    primary = TarzoAccent,
    onPrimary = TarzoDark,
    primaryContainer = Color(0xFF003D4D),
    onPrimaryContainer = Color(0xFF00D4FF),
    secondary = TarzoAccentSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFC4B5FD),
    tertiary = TarzoTeal,
    onTertiary = TarzoDark,
    tertiaryContainer = Color(0xFF042F2E),
    onTertiaryContainer = Color(0xFF5EEAD4),
    background = TarzoDark,
    onBackground = TarzoTextPrimary,
    surface = TarzoSurface,
    onSurface = TarzoTextPrimary,
    surfaceVariant = TarzoCard,
    onSurfaceVariant = TarzoTextSecondary,
    error = TarzoError,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFCA5A5),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    inverseSurface = TarzoTextPrimary,
    inverseOnSurface = TarzoDark,
    inversePrimary = TarzoAccentSecondary,
    scrim = TarzoScrim,
)

val TarzoLightColorScheme = lightColorScheme(
    primary = Color(0xFF0891B2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF003D4D),
    secondary = TarzoAccentSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = TarzoTeal,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF042F2E),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    error = TarzoError,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = TarzoAccent,
    scrim = Color(0x80000000),
)
