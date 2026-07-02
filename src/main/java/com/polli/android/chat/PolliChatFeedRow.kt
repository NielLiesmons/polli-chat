package com.polli.android.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.polli.android.permissions.BackgroundSetup

/** One feed row — used from [PolliChatFeedAdapter] bind (DC [ConversationItem.bind] equivalent). */
@Composable
fun PolliChatFeedRow(
    viewModel: ChatViewModel,
    msgId: Int,
    olderMsgId: Int?,
    newerMsgId: Int?,
    onQuoteClick: (Int) -> Unit,
    onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
) {
    val contentEpoch = viewModel.messageEpoch[msgId] ?: 0
    val msg = remember(msgId, contentEpoch) {
        viewModel.getChatMessage(msgId)
    } ?: return

    val layout = remember(msgId, olderMsgId, newerMsgId, contentEpoch) {
        viewModel.layoutForMessage(msgId, olderMsgId, newerMsgId)
    }
    val reactionReloadKey = viewModel.reactionEpochFor(msgId)
    val pulseEmoji = viewModel.reactionPulse?.takeIf { it.msgId == msg.id }?.emoji
    val context = LocalContext.current

    val openOverlay: (Offset) -> Unit = { tap ->
        if (!BackgroundSetup.tryHandleDeviceMessageTap(context, msg.id)) {
            onOpenMessageOverlay(msg, tap)
        }
    }

    if (msg.isOutgoing) {
        OutgoingMessageRow(
            message = msg,
            layout = layout,
            highlighted = viewModel.highlightId == msg.id,
            reactionReloadKey = reactionReloadKey,
            pulseEmoji = pulseEmoji,
            onSwipeReply = { viewModel.setReply(msg) },
            onClick = openOverlay,
            onQuoteClick = onQuoteClick,
        )
    } else {
        SingleIncomingMessageRow(
            message = msg,
            layout = layout,
            highlighted = viewModel.highlightId == msg.id,
            reactionReloadKey = reactionReloadKey,
            pulseEmoji = pulseEmoji,
            onSwipeReply = { viewModel.setReply(msg) },
            onClick = openOverlay,
            onQuoteClick = onQuoteClick,
        )
    }
}
