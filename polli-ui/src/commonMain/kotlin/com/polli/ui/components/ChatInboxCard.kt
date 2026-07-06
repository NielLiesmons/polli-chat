package com.polli.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.text.TextStyle
import com.polli.ui.components.polliClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.core.time.RelativeTimeFormat
import com.polli.core.chat.ChatCategory
import com.polli.domain.model.InboxItem
import com.polli.ui.components.InboxAvatarBadge
import com.polli.ui.components.InboxAvatarWithBadge
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.PolliDimens
import com.polli.ui.theme.accent

@Composable
fun ChatInboxCard(
    item: InboxItem,
    onClick: () -> Unit,
    nowSec: Long,
    modifier: Modifier = Modifier,
    avatar: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .polliClickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.width(PolliDimens.AvatarSize).padding(top = 2.dp)) {
                InboxAvatarWithBadge(
                    badge = inboxAvatarBadge(item.category),
                    avatarSize = PolliDimens.AvatarSize,
                ) {
                    avatar()
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 2.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = PolliDimens.InboxTitleRowMinHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.name,
                        color = PolliColors.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = RelativeTimeFormat.format(item.updatedAt, nowSec),
                        color = PolliColors.White33,
                        fontSize = 12.sp,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = PolliDimens.GroupNameNotifGap)
                        .heightIn(min = PolliDimens.InboxPreviewRowMinHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = inboxPreviewLine(item),
                        color = PolliColors.White66,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(PolliDimens.UnreadBadgeMinSize)
                                .clip(RoundedCornerShape(999.dp))
                                .background(accent().gradientBrush())
                                .padding(horizontal = PolliDimens.UnreadBadgeHPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                                color = PolliColors.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun inboxPreviewLine(item: InboxItem): String = buildString {
    item.previewAuthor?.let { append("$it: ") }
    append(if (item.preview.isBlank()) "No messages yet" else item.preview)
}

private fun inboxAvatarBadge(category: ChatCategory): InboxAvatarBadge? =
    when (category) {
        ChatCategory.Mail -> InboxAvatarBadge.Mail
        ChatCategory.Space -> null // spaces emoji badge — coming later
        else -> null
    }
