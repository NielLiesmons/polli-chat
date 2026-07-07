package com.polli.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.domain.model.InboxItem
import com.polli.ui.theme.PolliColors

fun formatHomeTabUnreadCount(count: Int): String? =
    when {
        count <= 0 -> null
        count > 99 -> "99+"
        else -> count.toString()
    }

/** Chats with new mail, newest first — feeds the Mail tab's author profile stack. */
fun mailUnreadChats(items: List<InboxItem>): List<InboxItem> =
    items
        .filter { it.unreadCount > 0 }
        .sortedByDescending { it.updatedAt }

fun totalUnreadMessages(items: List<InboxItem>): Int =
    items.sumOf { it.unreadCount.coerceAtLeast(0) }

/** Overlapping avatar stack (à la zapstore/webapp profile-stack widgets). */
@Composable
fun StackedTabAvatars(
    items: List<InboxItem>,
    avatarSize: Dp,
    overlap: Dp,
    maxVisible: Int = 3,
    chatAvatar: @Composable (InboxItem, Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = items.take(maxVisible)
    if (visible.isEmpty()) return
    val step = avatarSize - overlap
    val rowWidth = avatarSize + step * (visible.size - 1)
    Box(modifier = modifier.width(rowWidth).size(avatarSize)) {
        visible.forEachIndexed { index, item ->
            Box(
                modifier =
                    Modifier
                        .offset(x = step * index)
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(PolliColors.Black),
                contentAlignment = Alignment.Center,
            ) {
                chatAvatar(item, avatarSize - 2.dp)
            }
        }
    }
}
