package com.polli.android.chat

import com.polli.domain.model.chat.ChatMessage
import com.polli.core.chat.MessageGroupLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.android.theme.PolliColors
import com.polli.android.theme.accent
import com.polli.android.theme.PolliDimens
import com.polli.android.theme.ProfileColors
import com.polli.android.ui.PolliAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Max bubble width — 85% of the lane inside row gutters (matches polli web `max-width: 85%`). */
fun chatBubbleLaneMaxWidth(
    screenWidth: Dp,
    startGutter: Dp,
    endGutter: Dp,
    leadingReserved: Dp = 0.dp,
    trailingReserved: Dp = 0.dp,
): Dp {
    val available = screenWidth - startGutter - endGutter - leadingReserved - trailingReserved
    return (available * PolliDimens.ChatBubbleMaxWidthFraction).coerceAtLeast(0.dp)
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    layout: MessageGroupLayout,
    maxBubbleWidth: Dp,
    incomingInGroup: Boolean = false,
    reactions: List<BubbleReaction> = emptyList(),
    pulseEmoji: String? = null,
    onQuoteClick: ((Int) -> Unit)? = null,
) {
    val shape = bubbleShape(message.isOutgoing, layout.isLastInStack)
    val bg = if (message.isOutgoing) {
        accent().gradientBrush()
    } else {
        Brush.linearGradient(listOf(PolliColors.Gray66, PolliColors.Gray66))
    }
    val authorColor = ProfileColors.authorNameColor(message.authorColorSeed).copy(alpha = 0.85f)
    val timestamp = formatBubbleTime(message.timestamp)

    val hasQuote = message.quote != null
    val hasAttachment = message.hasAttachment
    val bodyHasText = message.text.isNotEmpty()
    val textOnlyOutgoing = message.isOutgoing && !hasQuote && !hasAttachment && bodyHasText

    val stackedExtra = if (
        !message.isOutgoing &&
        incomingInGroup &&
        !layout.isFirstInStack &&
        !hasQuote &&
        !hasAttachment
    ) {
        PolliDimens.ChatBubbleStackedIncomingTopExtra
    } else {
        0.dp
    }

    val shellPadding = PaddingValues(
        top = when {
            message.isOutgoing && textOnlyOutgoing ->
                PolliDimens.ChatBubblePaddingV + PolliDimens.ChatBubbleTextOnlyExtraTop
            message.isOutgoing -> PolliDimens.ChatBubblePaddingV
            else -> PolliDimens.ChatBubblePaddingV + stackedExtra
        },
        bottom = when {
            message.isOutgoing -> PolliDimens.ChatBubbleOutgoingShellBottom
            else -> PolliDimens.ChatBubblePaddingV + PolliDimens.ChatBubbleIncomingBottomExtra
        },
    )

    val insetH = PolliDimens.ChatBubbleInsetH
    val richContentPadH = PolliDimens.ChatQuoteBubblePadH
    val richContentWidth = maxBubbleWidth - richContentPadH * 2
    val quoteStyle = when {
        message.isOutgoing -> QuotedMessageStyle.InOutgoingBubble
        else -> QuotedMessageStyle.InIncomingBubble
    }

    val textOnlyBubble = !hasQuote && !hasAttachment && bodyHasText

    Box(
        modifier = Modifier
            .widthIn(max = maxBubbleWidth)
            .wrapContentWidth(if (message.isOutgoing) Alignment.End else Alignment.Start)
            .clip(shape)
            .background(bg, shape),
    ) {
        Column(
            modifier = Modifier
                .padding(shellPadding)
                .then(
                    when {
                        textOnlyBubble -> Modifier.width(IntrinsicSize.Max)
                        else -> Modifier.fillMaxWidth()
                    },
                ),
        ) {
            if (!message.isOutgoing && layout.isFirstInStack) {
                IncomingBubbleHeader(
                    authorName = message.authorName,
                    authorColor = authorColor,
                    timestamp = timestamp,
                    isEdited = message.isEdited,
                )
            }
            message.quote?.let { quote ->
                QuotedMessageBlock(
                    quote = quote,
                    style = quoteStyle,
                    modifier = Modifier.padding(horizontal = PolliDimens.ChatQuoteBubblePadH),
                    onClick = onQuoteClick?.let { cb -> { cb(quote.msgId) } },
                )
            }
            if (hasAttachment) {
                MessageMediaContent(
                    message = message,
                    contentWidth = richContentWidth,
                    isOutgoing = message.isOutgoing,
                    modifier = Modifier
                        .padding(horizontal = richContentPadH)
                        .padding(bottom = 4.dp),
                )
            }
            if (bodyHasText) {
                MessageBubbleText(
                    text = message.text,
                    isOutgoing = message.isOutgoing,
                    modifier = Modifier
                        .padding(horizontal = insetH)
                        .then(
                            if (textOnlyBubble) {
                                Modifier.width(IntrinsicSize.Max)
                            } else {
                                Modifier.fillMaxWidth()
                            },
                        ),
                )
            }
            if (reactions.isNotEmpty()) {
                ReactionPillsRow(
                    reactions = reactions,
                    pulseEmoji = pulseEmoji,
                    modifier = Modifier
                        .padding(horizontal = richContentPadH)
                        .consumeBubbleOverlayTap(),
                )
            }
            if (message.isOutgoing) {
                OutgoingBubbleMetaRow(
                    timestamp = timestamp,
                    state = message.outgoingState,
                    isEdited = message.isEdited,
                )
            }
        }
    }
}

@Composable
fun OutgoingMessageRow(
    message: ChatMessage,
    layout: MessageGroupLayout,
    maxBubbleWidth: Dp,
    highlighted: Boolean,
    reactions: List<BubbleReaction>,
    pulseEmoji: String?,
    onSwipeReply: () -> Unit,
    onClick: (Offset) -> Unit,
    onQuoteClick: (Int) -> Unit,
) {
    val rowStart = PolliDimens.ChatRowPaddingH + PolliDimens.ChatRowOutgoingExtraStart
    val rowEnd = PolliDimens.ChatRowPaddingH
    val rowTop = if (layout.isFirstInStack) PolliDimens.ChatRowTop else PolliDimens.ChatRowTopCollapsed
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = rowTop,
                start = rowStart,
                end = rowEnd,
            ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        BubbleSwiper(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .wrapContentWidth(Alignment.End)
                .messageRowHighlight(highlighted),
            alignEnd = true,
            replyIconInset = 4f,
            onSwipeReply = onSwipeReply,
            onTap = onClick,
        ) {
            MessageBubble(
                message = message,
                layout = layout,
                maxBubbleWidth = maxBubbleWidth,
                reactions = reactions,
                pulseEmoji = pulseEmoji,
                onQuoteClick = onQuoteClick,
            )
        }
    }
}

@Composable
fun SingleIncomingMessageRow(
    message: ChatMessage,
    layout: MessageGroupLayout,
    maxBubbleWidth: Dp,
    highlighted: Boolean,
    reactions: List<BubbleReaction>,
    pulseEmoji: String?,
    onSwipeReply: () -> Unit,
    onClick: (Offset) -> Unit,
    onQuoteClick: (Int) -> Unit,
) {
    val rowPad = PolliDimens.ChatRowPaddingH
    val incomingRight = PolliDimens.ChatRowIncomingRight
    val rowTop = if (layout.isFirstInStack) PolliDimens.ChatRowTop else PolliDimens.ChatRowTopCollapsed
    val showAvatar = layout.isLastInStack

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = rowPad,
                end = incomingRight,
                top = rowTop,
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            if (showAvatar) {
                PolliAvatar(
                    name = message.authorName,
                    seed = message.authorKey,
                    size = PolliDimens.ChatAvatarSize,
                    contactId = message.authorId,
                )
                Spacer(modifier = Modifier.width(PolliDimens.ChatAvatarGap))
            } else {
                Spacer(modifier = Modifier.width(PolliDimens.ChatIncomingGroupAvatarOffset))
            }
            BubbleSwiper(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .wrapContentWidth(Alignment.Start)
                    .messageRowHighlight(highlighted),
                alignEnd = false,
                replyIconInset = 8f,
                onSwipeReply = onSwipeReply,
                onTap = onClick,
            ) {
                MessageBubble(
                    message = message,
                    layout = layout,
                    maxBubbleWidth = maxBubbleWidth,
                    incomingInGroup = !layout.isFirstInStack,
                    reactions = reactions,
                    pulseEmoji = pulseEmoji,
                    onQuoteClick = onQuoteClick,
                )
            }
        }
    }
}

@Composable
private fun bubbleShape(isOutgoing: Boolean, isLastInStack: Boolean): RoundedCornerShape {
    val full = PolliDimens.ChatBubbleRadius
    val tail = PolliDimens.ChatBubbleTailRadius
    return if (isOutgoing) {
        if (isLastInStack) {
            RoundedCornerShape(topStart = full, topEnd = full, bottomEnd = tail, bottomStart = full)
        } else {
            RoundedCornerShape(full)
        }
    } else if (isLastInStack) {
        RoundedCornerShape(topStart = full, topEnd = full, bottomEnd = full, bottomStart = tail)
    } else {
        RoundedCornerShape(full)
    }
}

private val bubbleTimeFormat = ThreadLocal.withInitial {
    SimpleDateFormat("HH:mm", Locale.getDefault())
}

private fun formatBubbleTime(ts: Long): String {
    if (ts <= 0) return ""
    val millis = if (ts < 1_000_000_000_000L) ts * 1000 else ts
    return bubbleTimeFormat.get().format(Date(millis))
}
