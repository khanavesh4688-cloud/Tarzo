package com.tarzo.ai.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tarzo.ai.TarzoApp
import com.tarzo.ai.ui.screens.SettingsScreen
import com.tarzo.ai.ui.screens.SettingsScreenState
import com.tarzo.ai.ui.theme.*
import kotlinx.coroutines.launch

// ── Bottom Navigation Bar ──────────────────────────────────────────────

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
            SettingsScreenLive()
        }
    }
}

/**
 * Live Settings screen that reads/writes SecureStorage.
 */
@Composable
private fun SettingsScreenLive() {
    val context = LocalContext.current
    val app = context.applicationContext as TarzoApp
    val secureStorage = app.secureStorage
    val scope = rememberCoroutineScope()

    // Load initial values from SecureStorage
    var state by remember {
        mutableStateOf(
            SettingsScreenState(
                apiBaseUrl = secureStorage.getApiBaseUrl(),
                apiKey = secureStorage.getApiKey() ?: "",
            )
        )
    }

    SettingsScreen(
        state = state,
        onLanguageChange = { lang ->
            secureStorage.saveUserPreferredLanguage(lang)
            state = state.copy(languageCode = lang)
        },
        onTtsEngineChange = { engine ->
            state = state.copy(ttsEngine = engine)
        },
        onPitchChange = { pitch ->
            state = state.copy(voicePitch = pitch)
        },
        onSpeedChange = { speed ->
            state = state.copy(voiceSpeed = speed)
        },
        onWakeWordToggle = { enabled ->
            state = state.copy(isWakeWordEnabled = enabled)
        },
        onWakeWordSensitivityChange = { sensitivity ->
            state = state.copy(wakeWordSensitivity = sensitivity)
        },
        onApiBaseUrlChange = { url ->
            secureStorage.saveApiBaseUrl(url)
            state = state.copy(apiBaseUrl = url, isApiConnected = false)
        },
        onApiKeyChange = { key ->
            secureStorage.saveApiKey(key)
            state = state.copy(apiKey = key, isApiConnected = false)
        },
        onTestConnection = {
            scope.launch {
                // Simple test: just mark as connected if key and URL are present
                val hasKey = secureStorage.hasApiKey()
                val hasUrl = secureStorage.getApiBaseUrl().isNotBlank()
                state = state.copy(isApiConnected = hasKey && hasUrl)
            }
        },
        onNotificationToggle = { enabled ->
            state = state.copy(isNotificationsEnabled = enabled)
        },
        onThemeToggle = { dark ->
            state = state.copy(isDarkTheme = dark)
        },
        onNavigateToPrivacy = {},
    )
}

// ── Placeholder Screen ─────────────────────────────────────────────────

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