package com.polli.android.chat

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.android.theme.LabColors
import com.polli.android.theme.accent
import com.polli.android.theme.LabDimens
import com.polli.android.theme.ProfileColors
import com.polli.android.ui.LabAvatar
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
    return (available * LabDimens.ChatBubbleMaxWidthFraction).coerceAtLeast(0.dp)
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    layout: MessageGroupLayout,
    maxBubbleWidth: Dp,
    incomingInGroup: Boolean = false,
    reactionReloadKey: Int = 0,
    pulseEmoji: String? = null,
    onQuoteClick: ((Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    val shape = bubbleShape(message.isOutgoing, layout.isLastInStack)
    val bg = if (message.isOutgoing) {
        accent().gradientBrush()
    } else {
        Brush.linearGradient(listOf(LabColors.Gray66, LabColors.Gray66))
    }
    val reactions = remember(message.id, reactionReloadKey) {
        MessageReactions.loadReactionSummary(context, message.id)
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
        LabDimens.ChatBubbleStackedIncomingTopExtra
    } else {
        0.dp
    }

    val shellPadding = PaddingValues(
        top = when {
            message.isOutgoing && textOnlyOutgoing ->
                LabDimens.ChatBubblePaddingV + LabDimens.ChatBubbleTextOnlyExtraTop
            message.isOutgoing -> LabDimens.ChatBubblePaddingV
            else -> LabDimens.ChatBubblePaddingV + stackedExtra
        },
        bottom = when {
            message.isOutgoing -> LabDimens.ChatBubbleOutgoingShellBottom
            else -> LabDimens.ChatBubblePaddingV + LabDimens.ChatBubbleIncomingBottomExtra
        },
    )

    val insetH = LabDimens.ChatBubbleInsetH
    val richContentPadH = LabDimens.ChatQuoteBubblePadH
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
                    modifier = Modifier.padding(horizontal = LabDimens.ChatQuoteBubblePadH),
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
    highlighted: Boolean,
    reactionReloadKey: Int,
    pulseEmoji: String?,
    onSwipeReply: () -> Unit,
    onClick: (Offset) -> Unit,
    onQuoteClick: (Int) -> Unit,
) {
    val screenWidth = com.polli.ui.theme.layoutScreenWidthDp()
    val rowStart = LabDimens.ChatRowPaddingH + LabDimens.ChatRowOutgoingExtraStart
    val rowEnd = LabDimens.ChatRowPaddingH
    val maxBubbleWidth = chatBubbleLaneMaxWidth(
        screenWidth = screenWidth,
        startGutter = rowStart,
        endGutter = rowEnd,
    )
    val rowTop = if (layout.isFirstInStack) LabDimens.ChatRowTop else LabDimens.ChatRowTopCollapsed
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
                reactionReloadKey = reactionReloadKey,
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
    highlighted: Boolean,
    reactionReloadKey: Int,
    pulseEmoji: String?,
    onSwipeReply: () -> Unit,
    onClick: (Offset) -> Unit,
    onQuoteClick: (Int) -> Unit,
) {
    val screenWidth = com.polli.ui.theme.layoutScreenWidthDp()
    val rowPad = LabDimens.ChatRowPaddingH
    val incomingRight = LabDimens.ChatRowIncomingRight
    val rowTop = if (layout.isFirstInStack) LabDimens.ChatRowTop else LabDimens.ChatRowTopCollapsed
    val showAvatar = layout.isLastInStack
    val maxBubbleWidth = chatBubbleLaneMaxWidth(
        screenWidth = screenWidth,
        startGutter = rowPad,
        endGutter = incomingRight,
    )

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
                LabAvatar(
                    name = message.authorName,
                    seed = message.authorKey,
                    size = LabDimens.ChatAvatarSize,
                    contactId = message.authorId,
                )
                Spacer(modifier = Modifier.width(LabDimens.ChatAvatarGap))
            } else {
                Spacer(modifier = Modifier.width(LabDimens.ChatIncomingGroupAvatarOffset))
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
                    reactionReloadKey = reactionReloadKey,
                    pulseEmoji = pulseEmoji,
                    onQuoteClick = onQuoteClick,
                )
            }
        }
    }
}

@Composable
private fun bubbleShape(isOutgoing: Boolean, isLastInStack: Boolean): RoundedCornerShape {
    val full = LabDimens.ChatBubbleRadius
    val tail = LabDimens.ChatBubbleTailRadius
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

private fun formatBubbleTime(ts: Long): String {
    if (ts <= 0) return ""
    val millis = if (ts < 1_000_000_000_000L) ts * 1000 else ts
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
}
