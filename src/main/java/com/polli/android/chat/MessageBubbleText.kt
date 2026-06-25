package com.polli.android.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.polli.android.theme.LabColors
import kotlin.math.hypot

private const val LINK_TAP_SLOP_PX = 12f

@Composable
fun MessageBubbleText(
    text: String,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bodyColor = if (isOutgoing) LabColors.White else LabColors.White85
    val linkColor = if (isOutgoing) Color.White.copy(alpha = 0.92f) else LabColors.BlurpleLight
    val parts = remember(text) { MessageLinkify.splitMessageParts(text) }

    val annotated = remember(parts, isOutgoing) {
        buildAnnotatedString {
            parts.forEach { part ->
                when (part) {
                    is MessagePart.Text -> append(part.value)
                    is MessagePart.Link -> {
                        pushStringAnnotation(tag = "URL", annotation = part.href)
                        withStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ) {
                            append(oneLineUrlLabel(part.label))
                        }
                        pop()
                    }
                }
            }
        }
    }

    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    SelectionContainer {
        Text(
            text = annotated,
            modifier = modifier.pointerInput(annotated) {
                val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downUptime = down.uptimeMillis
                    val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                    if (up.isConsumed) return@awaitEachGesture
                    if (up.uptimeMillis - downUptime >= longPressTimeout) return@awaitEachGesture

                    val delta = up.position - down.position
                    if (hypot(delta.x, delta.y) > LINK_TAP_SLOP_PX) return@awaitEachGesture

                    val layout = textLayout ?: return@awaitEachGesture
                    val offset = layout.getOffsetForPosition(up.position)
                    val href = annotated
                        .getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()
                        ?.item
                        ?: return@awaitEachGesture

                    up.consume()
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(href)))
                }
            },
            style = TextStyle(
                color = bodyColor,
                fontSize = 14.5.sp,
                lineHeight = 19.5.sp,
                fontWeight = FontWeight.Normal,
            ),
            onTextLayout = { textLayout = it },
        )
    }
}

private fun oneLineUrlLabel(label: String): String {
    val flat = label.replace('\n', ' ').trim()
    return if (flat.length > 56) flat.take(53) + "…" else flat
}

/** One-line underlined URL label (standalone). */
@Composable
fun MessageLinkLine(
    url: String,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val linkColor = if (isOutgoing) Color.White.copy(alpha = 0.92f) else LabColors.BlurpleLight
    Text(
        text = oneLineUrlLabel(url),
        color = linkColor,
        fontSize = 13.sp,
        lineHeight = 19.5.sp,
        fontWeight = FontWeight.Normal,
        textDecoration = TextDecoration.Underline,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            },
        ),
    )
}
