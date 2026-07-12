package com.polli.android.chat

import com.polli.core.chat.MessageGroupLayout
import com.polli.domain.model.chat.ChatMessage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.polli.android.permissions.BackgroundSetup

/** One feed row — used from [PolliChatFeedAdapter] bind (DC [ConversationItem.bind] equivalent). */
@Composable
fun PolliChatFeedRow(
    viewModel: ChatViewModel,
    msgId: Int,
    groupLayout: MessageGroupLayout,
    maxBubbleWidth: Dp,
    highlighted: Boolean,
    reactionReloadKey: Int,
    pulseEmoji: String?,
    onQuoteClick: (Int) -> Unit,
    onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
) {
    val contentEpoch = viewModel.messageEpoch[msgId] ?: 0
    val msg =
        remember(msgId, contentEpoch) {
            viewModel.getChatMessage(msgId)
        }
    val stub =
        remember(msgId, contentEpoch) {
            viewModel.getStub(msgId)
        }
    val display = msg ?: stub?.toSkeletonChatMessage() ?: return

    val context = LocalContext.current

    val openOverlay: (Offset) -> Unit = { tap ->
        if (!BackgroundSetup.tryHandleDeviceMessageTap(context, display.id)) {
            onOpenMessageOverlay(display, tap)
        }
    }

    if (display.isOutgoing) {
        OutgoingMessageRow(
            message = display,
            layout = groupLayout,
            maxBubbleWidth = maxBubbleWidth,
            highlighted = highlighted,
            reactionReloadKey = reactionReloadKey,
            pulseEmoji = pulseEmoji,
            onSwipeReply = { viewModel.setReply(display) },
            onClick = openOverlay,
            onQuoteClick = onQuoteClick,
        )
    } else {
        SingleIncomingMessageRow(
            message = display,
            layout = groupLayout,
            maxBubbleWidth = maxBubbleWidth,
            highlighted = highlighted,
            reactionReloadKey = reactionReloadKey,
            pulseEmoji = pulseEmoji,
            onSwipeReply = { viewModel.setReply(display) },
            onClick = openOverlay,
            onQuoteClick = onQuoteClick,
        )
    }
}
