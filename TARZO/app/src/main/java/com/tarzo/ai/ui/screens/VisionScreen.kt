package com.tarzo.ai.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tarzo.ai.ui.theme.*

// ── Data models ─────────────────────────────────────────────────────────

/** Result of an image / OCR / screen analysis. */
data class VisionResult(
    val detectedText: String = "",
    val objects: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
)

/** Permission requirement for vision features. */
data class VisionPermissionStatus(
    val name: String,
    val isGranted: Boolean,
)

/** State for [VisionScreen]. */
data class VisionScreenState(
    val isAnalyzing: Boolean = false,
    val result: VisionResult? = null,
    val errorMessage: String? = null,
    val permissions: List<VisionPermissionStatus> = emptyList(),
    val hasGalleryImage: Boolean = false,
)

// ── Screen ─────────────────────────────────────────────────────────────

/**
 * Vision & analysis screen.
 *
 * Provides a camera preview placeholder, action buttons (Analyze Image,
 * OCR Text, Analyze Screen, Gallery Picker), results display, and
 * permission status indicators.
 *
 * @param state Current vision state.
 * @param onAnalyzeImage Trigger AI image analysis on the current frame.
 * @param onOcrText Trigger OCR text extraction on the current frame.
 * @param onAnalyzeScreen Trigger analysis of the current screen content.
 * @param onPickFromGallery Open the system gallery picker.
 * @param onRequestPermission Request a specific permission by name.
 */
@Composable
fun VisionScreen(
    state: VisionScreenState = remember { VisionScreenState() },
    onAnalyzeImage: () -> Unit = {},
    onOcrText: () -> Unit = {},
    onAnalyzeScreen: () -> Unit = {},
    onPickFromGallery: () -> Unit = {},
    onRequestPermission: (String) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TarzoDark)
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        // ── Header ────────────────────────────────────────────────
        item {
            Text(
                text = "Vision",
                color = TarzoTextPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Analyze images, extract text, and understand your screen",
                color = TarzoTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // ── Permission status indicators ──────────────────────────
        if (state.permissions.isNotEmpty()) {
            item {
                Text(
                    text = "Permissions",
                    color = TarzoTextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.permissions.forEach { perm ->
                        PermissionIndicator(
                            name = perm.name,
                            isGranted = perm.isGranted,
                            onClick = { if (!perm.isGranted) onRequestPermission(perm.name) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // ── Camera preview placeholder ────────────────────────────
        item {
            CameraPreviewPlaceholder(
                isAnalyzing = state.isAnalyzing,
                hasImage = state.hasGalleryImage,
            )
        }

        // ── Action buttons ────────────────────────────────────────
        item {
            ActionButtonRow(
                isAnalyzing = state.isAnalyzing,
                onAnalyzeImage = onAnalyzeImage,
                onOcrText = onOcrText,
                onAnalyzeScreen = onAnalyzeScreen,
            )
        }

        // ── Gallery picker ────────────────────────────────────────
        item {
            GalleryPickerButton(
                hasImage = state.hasGalleryImage,
                onClick = onPickFromGallery,
            )
        }

        // ── Error message ─────────────────────────────────────────
        if (state.errorMessage != null) {
            item {
                ErrorBanner(message = state.errorMessage)
            }
        }

        // ── Results display ───────────────────────────────────────
        if (state.result != null) {
            item {
                ResultsSection(result = state.result)
            }
        } else if (!state.isAnalyzing && state.errorMessage == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Take or select a photo to begin analysis",
                        color = TarzoTextSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ── Permission indicator chip ──────────────────────────────────────────

@Composable
private fun PermissionIndicator(
    name: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isGranted) TarzoSuccess.copy(alpha = 0.15f) else TarzoError.copy(alpha = 0.15f),
        animationSpec = tween(300),
        label = "perm_bg",
    )
    val fgColor = if (isGranted) TarzoSuccess else TarzoError

    Surface(
        modifier = modifier.clickable(enabled = !isGranted, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                color = fgColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Camera preview placeholder ─────────────────────────────────────────

@Composable
private fun CameraPreviewPlaceholder(
    isAnalyzing: Boolean,
    hasImage: Boolean,
) {
    val shimmerAlpha by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.02f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        TarzoSurface,
                        TarzoCard.copy(alpha = shimmerAlpha + 0.03f),
                        TarzoSurface,
                    ),
                    start = Offset.Zero,
                    end = Offset(600f, 600f),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isAnalyzing) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val rotation by rememberInfiniteTransition(label = "scan_spin").animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "scan_rotation",
                )
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Analyzing",
                    tint = TarzoAccent,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Analyzing...",
                    color = TarzoAccent,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else if (hasImage) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Image loaded",
                    tint = TarzoSuccess,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Image loaded",
                    color = TarzoSuccess,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera preview",
                    tint = TarzoTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Camera Preview",
                    color = TarzoTextSecondary.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// ── Action button row ──────────────────────────────────────────────────

@Composable
private fun ActionButtonRow(
    isAnalyzing: Boolean,
    onAnalyzeImage: () -> Unit,
    onOcrText: () -> Unit,
    onAnalyzeScreen: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VisionActionButton(
            icon = Icons.Default.AutoFixHigh,
            label = "Analyze Image",
            subtitle = "AI-powered image description and object detection",
            onClick = onAnalyzeImage,
            enabled = !isAnalyzing,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VisionActionButton(
                icon = Icons.Default.DocumentScanner,
                label = "OCR Text",
                subtitle = "Extract text from image",
                onClick = onOcrText,
                enabled = !isAnalyzing,
                modifier = Modifier.weight(1f),
            )
            VisionActionButton(
                icon = Icons.Default.Screenshot,
                label = "Analyze Screen",
                subtitle = "Read current screen",
                onClick = onAnalyzeScreen,
                enabled = !isAnalyzing,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VisionActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) TarzoCard else TarzoCard.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TarzoAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (enabled) TarzoAccent else TarzoTextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = if (enabled) TarzoTextPrimary else TarzoTextSecondary,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = subtitle,
                    color = TarzoTextSecondary.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Gallery picker button ──────────────────────────────────────────────

@Composable
private fun GalleryPickerButton(
    hasImage: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TarzoAccent.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TarzoAccent,
        ),
    ) {
        Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (hasImage) "Change Image from Gallery" else "Pick from Gallery",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

// ── Error banner ───────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TarzoError.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = TarzoError,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = TarzoError,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ── Results section ────────────────────────────────────────────────────

@Composable
private fun ResultsSection(result: VisionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = TarzoCard),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Results",
                color = TarzoTextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            HorizontalDivider(color = TarzoDivider)

            // Detected text.
            if (result.detectedText.isNotBlank()) {
                ResultRow(
                    label = "Detected Text",
                    value = result.detectedText,
                    icon = Icons.Default.TextFields,
                    color = TarzoAccent,
                )
            }

            // Detected objects.
            if (result.objects.isNotEmpty()) {
                ResultRow(
                    label = "Objects",
                    value = result.objects.joinToString(", "),
                    icon = Icons.Default.Category,
                    color = TarzoBlue,
                )
            }

            // Labels.
            if (result.labels.isNotEmpty()) {
                ResultRow(
                    label = "Labels",
                    value = result.labels.joinToString(", "),
                    icon = Icons.Default.Label,
                    color = TarzoAccentSecondary,
                )
            }

            if (result.detectedText.isBlank() && result.objects.isEmpty() && result.labels.isEmpty()) {
                Text(
                    text = "No results detected.",
                    color = TarzoTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = TarzoTextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value,
                color = TarzoTextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
