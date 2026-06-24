package com.polli.android.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.theme.ProfileColors
import com.polli.android.ui.LabAvatar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: ChatMessage,
    layout: MessageGroupLayout,
    incomingInGroup: Boolean = false,
    reactionReloadKey: Int = 0,
    pulseEmoji: String? = null,
    onQuoteClick: ((Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * LabDimens.ChatBubbleMaxWidthFraction
    val shape = bubbleShape(message.isOutgoing, layout.isLastInStack)
    val bg = if (message.isOutgoing) {
        Brush.linearGradient(listOf(LabColors.BlurpleGradientStart, LabColors.BlurpleGradientEnd))
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
    val quoteStyle = when {
        message.isOutgoing -> QuotedMessageStyle.InOutgoingBubble
        else -> QuotedMessageStyle.InIncomingBubble
    }

    Box(
        modifier = Modifier
            .widthIn(max = maxBubbleWidth)
            .clip(shape)
            .background(bg, shape),
    ) {
        Column(modifier = Modifier.padding(shellPadding)) {
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
                    modifier = Modifier.padding(horizontal = insetH),
                )
            }
            if (bodyHasText) {
                Text(
                    text = message.text,
                    color = if (message.isOutgoing) LabColors.White else LabColors.White85,
                    fontSize = 14.5.sp,
                    lineHeight = 19.5.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = insetH),
                )
            }
            if (reactions.isNotEmpty()) {
                ReactionPillsRow(
                    reactions = reactions,
                    pulseEmoji = pulseEmoji,
                    modifier = Modifier
                        .padding(horizontal = insetH)
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
fun IncomingMessageGroup(
    stack: List<Pair<ChatMessage, MessageGroupLayout>>,
    highlightedId: Int?,
    reactionReloadKey: Int,
    pulseEmojiFor: (Int) -> String?,
    onSwipeReply: (ChatMessage) -> Unit,
    onSwipeOptions: (ChatMessage, Rect) -> Unit,
    onBubbleClick: (ChatMessage, Rect) -> Unit,
    onQuoteClick: (Int) -> Unit,
) {
    if (stack.isEmpty()) return
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxGroupWidth = screenWidth * LabDimens.ChatBubbleMaxWidthFraction
    val firstLayout = stack.first().second
    val lastLayout = stack.last().second
    val lastMsg = stack.last().first
    val rowTop = if (firstLayout.isFirstInStack) LabDimens.ChatRowTop else LabDimens.ChatRowTopCollapsed
    var avatarSwipe by remember { mutableFloatStateOf(0f) }
    val avatarScale = 1f - avatarSwipe * 0.22f
    val avatarOpacity = 1f - avatarSwipe * 0.92f
    val showAvatar = lastLayout.isLastInStack

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = LabDimens.ChatRowPaddingH,
                end = LabDimens.ChatRowIncomingRight,
                top = rowTop,
            ),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(modifier = Modifier.widthIn(max = maxGroupWidth)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer {
                        scaleX = avatarScale
                        scaleY = avatarScale
                        alpha = avatarOpacity
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                            pivotFractionX = 0.5f,
                            pivotFractionY = 1f,
                        )
                    },
            ) {
                if (showAvatar) {
                    LabAvatar(
                        name = lastMsg.authorName,
                        seed = lastMsg.authorKey,
                        size = LabDimens.ChatAvatarSize,
                        contactId = lastMsg.authorId,
                    )
                } else {
                    Spacer(modifier = Modifier.size(LabDimens.ChatAvatarSize))
                }
            }
            Column(
                modifier = Modifier
                    .padding(start = LabDimens.ChatIncomingGroupAvatarOffset)
                    .widthIn(max = maxGroupWidth - LabDimens.ChatIncomingGroupAvatarOffset),
                verticalArrangement = Arrangement.spacedBy(LabDimens.ChatIncomingGroupBubbleGap),
            ) {
                stack.forEachIndexed { index, (msg, layout) ->
                    val isLastInGroup = index == stack.lastIndex
                    val drivesAvatar = isLastInGroup && showAvatar
                    BubbleSwiper(
                        modifier = Modifier.messageRowHighlight(highlightedId == msg.id),
                        alignEnd = false,
                        replyIconInset = 8f,
                        optionsIconInset = 8f,
                        onSwipeReply = { onSwipeReply(msg) },
                        onSwipeOptions = { bounds -> onSwipeOptions(msg, bounds) },
                        onTap = { bounds -> onBubbleClick(msg, bounds) },
                        onDragProgress = if (drivesAvatar) {
                            { dx: Float -> avatarSwipe = ((-dx).coerceIn(0f, 40f)) / 40f }
                        } else {
                            null
                        },
                    ) {
                        MessageBubble(
                            message = msg,
                            layout = layout,
                            incomingInGroup = true,
                            reactionReloadKey = reactionReloadKey,
                            pulseEmoji = pulseEmojiFor(msg.id),
                            onQuoteClick = onQuoteClick,
                        )
                    }
                }
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
    onSwipeOptions: (Rect) -> Unit,
    onClick: (Rect) -> Unit,
    onQuoteClick: (Int) -> Unit,
) {
    val rowTop = if (layout.isFirstInStack) LabDimens.ChatRowTop else LabDimens.ChatRowTopCollapsed
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = rowTop, start = LabDimens.ChatRowPaddingH, end = LabDimens.ChatRowPaddingH),
        contentAlignment = Alignment.CenterEnd,
    ) {
        BubbleSwiper(
            modifier = Modifier.messageRowHighlight(highlighted),
            alignEnd = true,
            replyIconInset = 4f,
            optionsIconInset = 4f,
            onSwipeReply = onSwipeReply,
            onSwipeOptions = onSwipeOptions,
            onTap = onClick,
        ) {
            MessageBubble(
                message = message,
                layout = layout,
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
    onSwipeOptions: (Rect) -> Unit,
    onClick: (Rect) -> Unit,
    onQuoteClick: (Int) -> Unit,
) {
    val rowTop = if (layout.isFirstInStack) LabDimens.ChatRowTop else LabDimens.ChatRowTopCollapsed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = LabDimens.ChatRowPaddingH,
                end = LabDimens.ChatRowIncomingRight,
                top = rowTop,
            ),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (layout.isLastInStack) {
            LabAvatar(
                name = message.authorName,
                seed = message.authorKey,
                size = LabDimens.ChatAvatarSize,
                contactId = message.authorId,
            )
            Spacer(modifier = Modifier.size(LabDimens.ChatAvatarGap))
        } else {
            Spacer(modifier = Modifier.size(LabDimens.ChatIncomingGroupAvatarOffset))
        }
        BubbleSwiper(
            modifier = Modifier.messageRowHighlight(highlighted),
            alignEnd = false,
            replyIconInset = 50f,
            optionsIconInset = 8f,
            onSwipeReply = onSwipeReply,
            onSwipeOptions = onSwipeOptions,
            onTap = onClick,
        ) {
            MessageBubble(
                message = message,
                layout = layout,
                incomingInGroup = false,
                reactionReloadKey = reactionReloadKey,
                pulseEmoji = pulseEmoji,
                onQuoteClick = onQuoteClick,
            )
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
