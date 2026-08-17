package com.tarzo.ai.ui.components

import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tarzo.ai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Identifies who authored a message in the conversation.
 */
enum class MessageAuthor {
    /** The human user. */
    USER,
    /** TARZO (the AI assistant). */
    TARZO,
}

/**
 * A single message in the conversation history.
 *
 * @param id Unique identifier for this message.
 * @param text The raw message body (may contain markdown-like markers).
 * @param author Who sent the message.
 * @param timestamp Epoch millis when the message was created.
 */
data class ConversationMessage(
    val id: Long,
    val text: String,
    val author: MessageAuthor,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * A chat bubble for the conversation history.
 *
 * User messages are right-aligned with a dark-blue background.
 * TARZO responses are left-aligned with a dark-gray card background
 * and a cyan accent left border.
 *
 * Supports lightweight markdown-like formatting:
 * - `**bold**` → bold text
 * - `*italic*` → italic text
 * - `` `code` `` → monospace text
 *
 * @param message The [ConversationMessage] to display.
 * @param modifier Modifier applied to the outer bubble.
 */
@Composable
fun ConversationBubble(
    message: ConversationMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.author == MessageAuthor.USER

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (isUser) {
            // User bubble — right aligned
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 4.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp,
                            ),
                        )
                        .background(Color(0xFF1E3A5F)),
                ) {
                    Text(
                        text = parseMarkdown(message.text),
                        color = TarzoTextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                TimestampLabel(message.timestamp, isUser = true)
            }
        } else {
            // TARZO bubble — left aligned with accent border
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                Box(
                    modifier = Modifier
                        .drawBehind {
                            // Left accent border (4 dp wide)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(TarzoAccent, TarzoAccentSecondary),
                                ),
                                size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height),
                            )
                        }
                        .clip(
                            RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp,
                            ),
                        )
                        .background(TarzoCard),
                ) {
                    Text(
                        text = parseMarkdown(message.text),
                        color = TarzoTextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                    )
                }
                TimestampLabel(message.timestamp, isUser = false)
            }
        }
    }
}

/**
 * Small timestamp shown below a bubble.
 */
@Composable
private fun TimestampLabel(timestamp: Long, isUser: Boolean) {
    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    Text(
        text = sdf.format(Date(timestamp)),
        color = TarzoTextSecondary,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 2.dp, start = if (isUser) 0.dp else 8.dp, end = if (isUser) 4.dp else 0.dp),
        textAlign = if (isUser) TextAlign.End else TextAlign.Start,
    )
}

/**
 * Lightweight markdown parser that converts:
 * - `**text**` → bold
 * - `*text*` → italic (must not be preceded/followed by `*`)
 * - `` `text` `` → monospace
 */
private fun parseMarkdown(raw: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < raw.length) {
            // Bold: **text**
            if (i + 1 < raw.length && raw[i] == '*' && raw[i + 1] == '*') {
                val end = raw.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(raw.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            // Italic: *text* (single asterisk)
            if (raw[i] == '*' && (i == 0 || raw[i - 1] != '*')) {
                val end = raw.indexOf('*', i + 1)
                if (end != -1 && (end + 1 >= raw.length || raw[end + 1] != '*')) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Light)) {
                        append(raw.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            // Code: `text`
            if (raw[i] == '`') {
                val end = raw.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = TarzoSurfaceOverlay)) {
                        append(raw.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            // Newlines → compose newlines
            if (raw[i] == '\n') {
                append('\n')
                i++
                continue
            }
            append(raw[i])
            i++
        }
    }
}
