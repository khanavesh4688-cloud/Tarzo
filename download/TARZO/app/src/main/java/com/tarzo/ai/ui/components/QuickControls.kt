package com.tarzo.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tarzo.ai.ui.theme.*

/**
 * Data class describing a single quick-action button.
 *
 * @param icon Material icon to display inside the circle.
 * @param label Short label rendered below the icon.
 * @param tintColour Icon tint colour. Defaults to [TarzoAccent].
 * @param onClick Callback invoked when the button is tapped.
 * @param isEnabled Visual enabled state. When false the button is dimmed.
 */
data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val tintColour: Color = TarzoAccent,
    val onClick: () -> Unit = {},
    val isEnabled: Boolean = true,
)

/**
 * Pre-defined quick actions for TARZO's home screen.
 */
object DefaultQuickActions {
    fun flashLight(isOn: Boolean, onClick: () -> Unit) = QuickAction(
        icon = if (isOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
        label = "Flash",
        tintColour = if (isOn) TarzoWarning else TarzoAccent,
        onClick = onClick,
    )

    fun camera(onClick: () -> Unit) = QuickAction(
        icon = Icons.Default.CameraAlt,
        label = "Camera",
        tintColour = TarzoPink,
        onClick = onClick,
    )

    fun wifi(isOn: Boolean, onClick: () -> Unit) = QuickAction(
        icon = if (isOn) Icons.Default.Wifi else Icons.Default.WifiOff,
        label = "WiFi",
        tintColour = if (isOn) TarzoSuccess else TarzoTextSecondary,
        onClick = onClick,
    )

    fun bluetooth(isOn: Boolean, onClick: () -> Unit) = QuickAction(
        icon = if (isOn) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
        label = "Bluetooth",
        tintColour = if (isOn) TarzoBlue else TarzoTextSecondary,
        onClick = onClick,
    )

    fun volume(onClick: () -> Unit) = QuickAction(
        icon = Icons.Default.VolumeUp,
        label = "Volume",
        tintColour = TarzoOrange,
        onClick = onClick,
    )

    fun brightness(onClick: () -> Unit) = QuickAction(
        icon = Icons.Default.BrightnessHigh,
        label = "Bright",
        tintColour = TarzoYellow,
        onClick = onClick,
    )
}

/**
 * Horizontal row of quick-action circular buttons.
 *
 * Each button shows a tinted icon inside a dark circle with a
 * small label underneath. The row scrolls horizontally when
 * content exceeds the available width.
 *
 * @param actions List of [QuickAction] items to render.
 * @param modifier Modifier applied to the outer row.
 * @param buttonSize Diameter of each circular button.
 */
@Composable
fun QuickControls(
    actions: List<QuickAction>,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 56.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { action ->
            QuickActionButton(
                action = action,
                size = buttonSize,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

/**
 * A single quick-action circular button.
 */
@Composable
private fun QuickActionButton(
    action: QuickAction,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val alpha = if (action.isEnabled) 1f else 0.4f

    Column(
        modifier = modifier
            .clickable(enabled = action.isEnabled, onClick = action.onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(TarzoCard.copy(alpha = alpha)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = action.tintColour.copy(alpha = alpha),
                modifier = Modifier.size(size * 0.45f),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = action.label,
            color = TarzoTextSecondary.copy(alpha = alpha),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
