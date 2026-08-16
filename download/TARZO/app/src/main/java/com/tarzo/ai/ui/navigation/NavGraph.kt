package com.tarzo.ai.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tarzo.ai.ui.theme.*

// ── Bottom Navigation Bar ──────────────────────────────────────────────

/**
 * TARZO bottom navigation bar.
 *
 * Renders the primary tab icons with the cyan accent for the
 * selected item and a translucent surface background.
 *
 * @param navController Used to observe and change the current route.
 * @param modifier Applied to the outer container.
 */
@Composable
fun TarzoBottomNavBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        containerColor = TarzoNavBackground,
        contentColor = TarzoNavUnselected,
        tonalElevation = 0.dp,
    ) {
        Route.bottomNavItems.forEach { route ->
            val selected = currentRoute == route.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = route.icon,
                        contentDescription = route.label,
                    )
                },
                label = {
                    Text(
                        text = route.label,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                selected = selected,
                onClick = {
                    // Avoid re-navigating to the same route.
                    if (currentRoute != route.route) {
                        navController.navigate(route.route) {
                            popUpTo(Route.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TarzoNavSelected,
                    selectedTextColor = TarzoNavSelected,
                    unselectedIconColor = TarzoNavUnselected,
                    unselectedTextColor = TarzoNavUnselected,
                    indicatorColor = TarzoAccent.copy(alpha = 0.12f),
                ),
            )
        }
    }
}

// ── Navigation Host ────────────────────────────────────────────────────

/**
 * The main TARZO navigation graph.
 *
 * Wires every [Route] to a placeholder destination composable.
 * Replace each `PlaceholderScreen` with the real feature screen
 * as it is built.
 *
 * @param navController The [NavHostController].
 * @param startDestination The initial route (defaults to [Route.Home]).
 * @param modifier Applied to the NavHost container.
 */
@Composable
fun TarzoNavGraph(
    navController: NavHostController,
    startDestination: String = Route.Home.route,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
    ) {
        composable(Route.Home.route) {
            PlaceholderScreen(
                title = "Home",
                subtitle = "Voice assistant — say \"Bolo TARZO\"",
                icon = Icons.Default.Home,
            )
        }
        composable(Route.Voice.route) {
            PlaceholderScreen(
                title = "Voice",
                subtitle = "Conversation history & voice controls",
                icon = Icons.Default.Mic,
            )
        }
        composable(Route.Automation.route) {
            PlaceholderScreen(
                title = "Automation",
                subtitle = "Device routines & rules",
                icon = Icons.Default.AutoMode,
            )
        }
        composable(Route.Vision.route) {
            PlaceholderScreen(
                title = "Vision",
                subtitle = "Camera, OCR & object detection",
                icon = Icons.Default.CameraAlt,
            )
        }
        composable(Route.Memory.route) {
            PlaceholderScreen(
                title = "Memory",
                subtitle = "Browse & search stored memories",
                icon = Icons.Default.Memory,
            )
        }
        composable(Route.Security.route) {
            PlaceholderScreen(
                title = "Security",
                subtitle = "Anti-theft & privacy settings",
                icon = Icons.Default.Security,
            )
        }
        composable(Route.Settings.route) {
            PlaceholderScreen(
                title = "Settings",
                subtitle = "Language, voice, wake word & more",
                icon = Icons.Default.Settings,
            )
        }
    }
}

// ── Placeholder Screen ─────────────────────────────────────────────────

/**
 * Temporary placeholder that renders a route's title, subtitle and icon.
 *
 * This will be replaced by the actual feature screens in subsequent tasks.
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TarzoAccent.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = TarzoTextPrimary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = TarzoTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ── Scaffold wrapper ───────────────────────────────────────────────────

/**
 * Full-screen scaffold with the bottom navigation bar.
 *
 * Use this as the root composable for the main activity.
 * It hosts [TarzoNavGraph] and [TarzoBottomNavBar].
 *
 * @param navController The [NavHostController].
 * @param modifier Applied to the outer scaffold.
 */
@Composable
fun TarzoScaffold(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = { TarzoBottomNavBar(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
    ) { innerPadding ->
        TarzoNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
