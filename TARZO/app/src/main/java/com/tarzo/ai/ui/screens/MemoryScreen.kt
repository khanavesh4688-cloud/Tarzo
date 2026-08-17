package com.tarzo.ai.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tarzo.ai.core.storage.MemoryItem
import com.tarzo.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ── Screen ─────────────────────────────────────────────────────────────

/**
 * Persistent memory management screen.
 *
 * Displays memories grouped by category, supports searching,
 * adding, deleting individual items, and clearing all.
 *
 * @param memories The full list of [MemoryItem] to display.
 * @param onAddMemory Called with the content and category string.
 * @param onDeleteMemory Called with the memory ID to delete.
 * @param onClearAll Called when the user confirms clearing all memories.
 */
@Composable
fun MemoryScreen(
    memories: List<MemoryItem> = emptyList(),
    onAddMemory: (content: String, category: String) -> Unit = { _, _ -> },
    onDeleteMemory: (Long) -> Unit = {},
    onClearAll: () -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val filtered = remember(memories, searchQuery) {
        if (searchQuery.isBlank()) memories
        else memories.filter { it.content.contains(searchQuery, ignoreCase = true) }
    }

    val grouped = remember(filtered) {
        filtered.groupBy { it.category }
    }

    val categoryOrder = listOf(
        MemoryItem.CATEGORY_PREFERENCE,
        MemoryItem.CATEGORY_FACT,
        MemoryItem.CATEGORY_CONTACT,
        MemoryItem.CATEGORY_SETTING,
    )
    val sortedCategories = remember(grouped.keys) {
        grouped.keys.sortedBy { cat -> categoryOrder.indexOf(cat).coerceAtLeast(categoryOrder.size) }
    }

    // Stats.
    val categoryCounts = remember(memories) {
        mapOf(
            MemoryItem.CATEGORY_PREFERENCE to memories.count { it.category == MemoryItem.CATEGORY_PREFERENCE },
            MemoryItem.CATEGORY_FACT to memories.count { it.category == MemoryItem.CATEGORY_FACT },
            MemoryItem.CATEGORY_CONTACT to memories.count { it.category == MemoryItem.CATEGORY_CONTACT },
            MemoryItem.CATEGORY_SETTING to memories.count { it.category == MemoryItem.CATEGORY_SETTING },
        )
    }

    // Dialogs.
    if (showAddDialog) {
        AddMemoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { content, category ->
                onAddMemory(content, category)
                showAddDialog = false
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Clear All Memories?",
                    color = TarzoTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text = "This action cannot be undone. All ${memories.size} memories will be permanently deleted.",
                    color = TarzoTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    showClearDialog = false
                }) {
                    Text("Clear All", color = TarzoError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TarzoTextSecondary)
                }
            },
            containerColor = TarzoSurface,
            shape = MaterialTheme.shapes.medium,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TarzoDark)
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        // ── Header ────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
 Text(
                text = "Memory",
                color = TarzoTextPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.weight(1f))
            // Stats summary.
            Text(
                text = "${memories.size} total",
                color = TarzoTextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Things TARZO remembers about you",
            color = TarzoTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ── Stats row ─────────────────────────────────────────────
        StatsRow(categoryCounts = categoryCounts)

        Spacer(modifier = Modifier.height(14.dp))

        // ── Search bar ────────────────────────────────────────────
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Action buttons ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = TarzoAccent.copy(alpha = 0.15f),
                    contentColor = TarzoAccent,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Memory", style = MaterialTheme.typography.labelLarge)
            }
            if (memories.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TarzoError.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TarzoError),
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Memory list ───────────────────────────────────────────
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = TarzoTextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No memories match \"$searchQuery\""
                        else "No memories stored yet",
                        color = TarzoTextSecondary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                sortedCategories.forEach { category ->
                    val items = grouped[category] ?: emptyList()
                    item {
                        CategoryHeader(
                            category = category,
                            count = items.size,
                        )
                    }
                    items(
                        items = items,
                        key = { it.id },
                    ) { memory ->
                        MemoryItemRow(
                            item = memory,
                            onDelete = { onDeleteMemory(memory.id) },
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Stats row ──────────────────────────────────────────────────────────

@Composable
private fun StatsRow(categoryCounts: Map<String, Int>) {
    val entries = listOf(
        Triple(MemoryItem.CATEGORY_PREFERENCE, "Preference", TarzoAccent),
        Triple(MemoryItem.CATEGORY_FACT, "Fact", TarzoBlue),
        Triple(MemoryItem.CATEGORY_CONTACT, "Contact", TarzoSuccess),
        Triple(MemoryItem.CATEGORY_SETTING, "Setting", TarzoOrange),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        entries.forEach { (key, label, color) ->
            val count = categoryCounts[key] ?: 0
            StatChip(label = label, count = count, color = color, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$count",
                color = color,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = label,
                color = color.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

// ── Search bar ─────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = "Search memories...",
                color = TarzoTextSecondary.copy(alpha = 0.5f),
            )
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = TarzoTextSecondary)
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TarzoTextSecondary)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TarzoTextPrimary,
            unfocusedTextColor = TarzoTextPrimary,
            cursorColor = TarzoAccent,
            focusedBorderColor = TarzoAccent,
            unfocusedBorderColor = TarzoDivider,
            focusedContainerColor = TarzoCard,
            unfocusedContainerColor = TarzoCard,
        ),
    )
}

// ── Category header ────────────────────────────────────────────────────

@Composable
private fun CategoryHeader(
    category: String,
    count: Int,
) {
    val color = when (category) {
        MemoryItem.CATEGORY_PREFERENCE -> TarzoAccent
        MemoryItem.CATEGORY_FACT -> TarzoBlue
        MemoryItem.CATEGORY_CONTACT -> TarzoSuccess
        MemoryItem.CATEGORY_SETTING -> TarzoOrange
        else -> TarzoTextSecondary
    }
    val icon = when (category) {
        MemoryItem.CATEGORY_PREFERENCE -> Icons.Default.Favorite
        MemoryItem.CATEGORY_FACT -> Icons.Default.Lightbulb
        MemoryItem.CATEGORY_CONTACT -> Icons.Default.Person
        MemoryItem.CATEGORY_SETTING -> Icons.Default.Settings
        else -> Icons.Default.Label
    }

    Row(
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category.lowercase().replaceFirstChar { it.uppercase() },
            color = color,
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($count)",
            color = TarzoTextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

// ── Memory item row ────────────────────────────────────────────────────

@Composable
private fun MemoryItemRow(
    item: MemoryItem,
    onDelete: () -> Unit,
) {
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    val badgeColor = when (item.category) {
        MemoryItem.CATEGORY_PREFERENCE -> TarzoAccent
        MemoryItem.CATEGORY_FACT -> TarzoBlue
        MemoryItem.CATEGORY_CONTACT -> TarzoSuccess
        MemoryItem.CATEGORY_SETTING -> TarzoOrange
        else -> TarzoTextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = TarzoCard),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Category badge.
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor.copy(alpha = 0.15f),
            ) {
                Text(
                    text = item.category.take(3),
                    color = badgeColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.content,
                    color = TarzoTextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = sdf.format(Date(item.timestamp)),
                    color = TarzoTextSecondary.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = TarzoTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ── Add memory dialog ──────────────────────────────────────────────────

@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (content: String, category: String) -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MemoryItem.CATEGORY_FACT) }
    var expanded by remember { mutableStateOf(false) }

    val categories = listOf(
        MemoryItem.CATEGORY_PREFERENCE,
        MemoryItem.CATEGORY_FACT,
        MemoryItem.CATEGORY_CONTACT,
        MemoryItem.CATEGORY_SETTING,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Memory",
                color = TarzoTextPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What should I remember?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TarzoTextPrimary,
                        cursorColor = TarzoAccent,
                        focusedBorderColor = TarzoAccent,
                        unfocusedBorderColor = TarzoDivider,
                        focusedContainerColor = TarzoCard,
                        unfocusedContainerColor = TarzoCard,
                    ),
                    maxLines = 4,
                )

                Box {
                    OutlinedTextField(
                        value = selectedCategory.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TarzoTextPrimary,
                            focusedBorderColor = TarzoAccent,
                            unfocusedBorderColor = TarzoDivider,
                            focusedContainerColor = TarzoCard,
                            unfocusedContainerColor = TarzoCard,
                        ),
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TarzoTextSecondary)
                        },
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = TarzoSurface,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.lowercase().replaceFirstChar { it.uppercase() }, color = TarzoTextPrimary) },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (content.isNotBlank()) onConfirm(content, selectedCategory) },
                enabled = content.isNotBlank(),
            ) {
                Text("Save", color = TarzoAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TarzoTextSecondary)
            }
        },
        containerColor = TarzoSurface,
        shape = MaterialTheme.shapes.medium,
    )
}