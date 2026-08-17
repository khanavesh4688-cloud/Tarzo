package com.tarzo.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tarzo.ai.ui.theme.*

/**
 * Permission status shown on the card.
 */
enum class PermissionStatus {
    /** Permission has been granted. */
    GRANTED,
    /** Permission was denied by the user. */
    DENIED,
    /** Permission has not been requested yet. */
    NOT_ASKED,
}

/**
 * Data class describing a permission entry for display.
 *
 * @param name Human-readable group name (e.g. "Microphone").
 * @param description Why TARZO needs this permission.
 * @param status Current [PermissionStatus].
 * @param icon Material icon for the permission group.
 * @param onRequest Callback to request the permission.
 */
data class PermissionEntry(
    val name: String,
    val description: String,
    val status: PermissionStatus,
    val icon: ImageVector = Icons.Default.Shield,
    val onRequest: () -> Unit = {},
)

/**
 * A card that displays a single permission group with its current
 * status and a request button.
 *
 * The card background is [TarzoCard], the left edge shows a colour-coded
 * status stripe, and the right side has a contextual action button.
 *
 * @param entry The [PermissionEntry] to render.
 * @param modifier Modifier applied to the card.
 */
@Composable
fun PermissionCard(
    entry: PermissionEntry,
    modifier: Modifier = Modifier,
) {
    // Status-aware colours.
    val statusColor by animateColorAsState(
        targetValue = when (entry.status) {
            PermissionStatus.GRANTED -> TarzoSuccess
            PermissionStatus.DENIED -> TarzoError
            PermissionStatus.NOT_ASKED -> TarzoWarning
        },
        label = "perm_status_color",
    )

    val statusIcon: ImageVector = when (entry.status) {
        PermissionStatus.GRANTED -> Icons.Default.CheckCircle
        PermissionStatus.DENIED -> Icons.Default.Cancel
        PermissionStatus.NOT_ASKED -> Icons.Default.HelpOutline
    }

    val statusLabel: String = when (entry.status) {
        PermissionStatus.GRANTED -> "Granted"
        PermissionStatus.DENIED -> "Denied"
        PermissionStatus.NOT_ASKED -> "Not asked"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = TarzoCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(start = 0.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status stripe (4 dp wide).
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(statusColor),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Icon.
            Icon(
                imageVector = entry.icon,
                contentDescription = entry.name,
                tint = TarzoTextSecondary,
                modifier = Modifier.size(28.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Name + description.
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = entry.name,
                    color = TarzoTextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.description,
                    color = TarzoTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right column: status badge + button.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = statusLabel,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusLabel,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                )
                if (entry.status != PermissionStatus.GRANTED) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = entry.onRequest,
                        colors = ButtonDefaults.textButtonColors(contentColor = TarzoAccent),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = if (entry.status == PermissionStatus.NOT_ASKED) "Allow" else "Re-ask",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Convenience composable that renders a list of [PermissionEntry] items.
 *
 * @param permissions The list of permission entries.
 * @param modifier Modifier applied to the column.
 */
@Composable
fun PermissionList(
    permissions: List<PermissionEntry>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        permissions.forEach { entry ->
            PermissionCard(entry = entry)
        }
    }
}
