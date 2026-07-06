package com.polli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.FeedItem
import com.polli.ui.chat.ChatController
import com.polli.ui.components.chat.ChatDayMarkerPill
import com.polli.ui.components.chat.ChatNewMessagesPill
import com.polli.ui.components.DetailScreenHeader
import com.polli.ui.components.PolliPrimaryButton
import com.polli.ui.components.RoundBackButton
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.PolliDimens
import com.polli.ui.theme.ProfileColors

/**
 * Shared chat screen — feed + composer on [MessageRepository] via [ChatController].
 * Android uses a richer RecyclerView host; desktop and tests use this directly.
 */
@Composable
fun ChatScreen(
    controller: ChatController,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    newMessagesLabel: String = "New messages",
) {
    val feedItems by controller::feedItems
    val draft by controller::draft
    val highlightId by controller::highlightId
    val reloadGeneration by controller::reloadGeneration
    val listState = rememberLazyListState()

    LaunchedEffect(reloadGeneration, feedItems.size) {
        if (controller.consumeScrollToBottomOnReload() && feedItems.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(controller.pendingFirstLoadScroll, feedItems.size) {
        if (controller.pendingFirstLoadScroll && feedItems.isNotEmpty()) {
            val target = controller.initialScrollIndex.coerceIn(0, feedItems.lastIndex)
            listState.scrollToItem(target)
            controller.clearFirstLoadScroll()
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(PolliColors.Black),
    ) {
        DetailScreenHeader(
            title = title,
            backButton = { RoundBackButton(onClick = onBack) },
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize().padding(horizontal = PolliDimens.HomeBarPadding),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(
                    items = feedItems,
                    key = { _, item ->
                        when (item) {
                            is FeedItem.DayMarker -> "day-${item.dayKey}"
                            FeedItem.NewMessages -> "new-messages"
                            is FeedItem.Message -> "msg-${item.msgId}-$reloadGeneration"
                        }
                    },
                ) { _, item ->
                    when (item) {
                        is FeedItem.DayMarker ->
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                ChatDayMarkerPill(label = item.label)
                            }
                        FeedItem.NewMessages ->
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                ChatNewMessagesPill(label = newMessagesLabel)
                            }
                        is FeedItem.Message -> {
                            val msg = controller.getChatMessage(item.msgId)
                            if (msg != null) {
                                SharedMessageBubble(
                                    message = msg,
                                    highlighted = highlightId == msg.id,
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = PolliDimens.HomeBarPadding, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = controller::updateDraft,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message", color = PolliColors.White33) },
                maxLines = 6,
            )
            PolliPrimaryButton(
                label = "Send",
                onClick = controller::send,
                enabled = draft.trim().isNotEmpty(),
            )
        }
    }
}

@Composable
private fun SharedMessageBubble(
    message: ChatMessage,
    highlighted: Boolean,
) {
    val bubbleColor =
        if (message.isOutgoing) {
            PolliColors.White16
        } else {
            ProfileColors.stringToColor(message.authorKey).copy(alpha = 0.35f)
        }
    val align = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = align) {
        Column(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (highlighted) bubbleColor.copy(alpha = 0.9f) else bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(0.85f),
        ) {
            if (!message.isOutgoing && message.authorName.isNotBlank()) {
                Text(
                    text = message.authorName,
                    color = ProfileColors.authorNameColor(message.authorKey),
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            if (message.hasAttachment && message.text.isBlank()) {
                Text(
                    text = message.fileName ?: "[attachment]",
                    color = PolliColors.White85,
                )
            }
            if (message.text.isNotBlank()) {
                Text(text = message.text, color = PolliColors.White85)
            }
        }
    }
}
