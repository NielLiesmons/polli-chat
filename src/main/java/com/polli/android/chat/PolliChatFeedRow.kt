package com.polli.android.chat

import com.polli.core.chat.MessageGroupLayout
import com.polli.domain.model.chat.ChatMessage
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.polli.android.permissions.BackgroundSetup

/** One feed row — message resolved in [PolliChatFeedAdapter.Holder.bind] like DC [ConversationItem.bind]. */
@Composable
fun PolliChatFeedRow(
    message: ChatMessage,
    groupLayout: MessageGroupLayout,
    maxBubbleWidth: Dp,
    highlighted: Boolean,
    reactions: List<BubbleReaction>,
    pulseEmoji: String?,
    onSwipeReply: () -> Unit,
    onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
    onQuoteClick: (Int) -> Unit,
) {
    val context = LocalContext.current

    val openOverlay: (Offset) -> Unit = { tap ->
        if (!BackgroundSetup.tryHandleDeviceMessageTap(context, message.id)) {
            onOpenMessageOverlay(message, tap)
        }
    }

    if (message.isOutgoing) {
        OutgoingMessageRow(
            message = message,
            layout = groupLayout,
            maxBubbleWidth = maxBubbleWidth,
            highlighted = highlighted,
            reactions = reactions,
            pulseEmoji = pulseEmoji,
            onSwipeReply = onSwipeReply,
            onClick = openOverlay,
            onQuoteClick = onQuoteClick,
        )
    } else {
        SingleIncomingMessageRow(
            message = message,
            layout = groupLayout,
            maxBubbleWidth = maxBubbleWidth,
            highlighted = highlighted,
            reactions = reactions,
            pulseEmoji = pulseEmoji,
            onSwipeReply = onSwipeReply,
            onClick = openOverlay,
            onQuoteClick = onQuoteClick,
        )
    }
}
