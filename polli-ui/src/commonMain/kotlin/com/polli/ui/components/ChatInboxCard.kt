package com.polli.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.polli.domain.model.InboxItem
import com.polli.ui.theme.LabColors
import com.polli.ui.theme.LabDimens
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
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.width(LabDimens.AvatarSize).padding(top = 2.dp)) {
                avatar()
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 2.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = LabDimens.InboxTitleRowMinHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.name,
                        color = LabColors.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = RelativeTimeFormat.format(item.updatedAt, nowSec),
                        color = LabColors.White33,
                        fontSize = 12.sp,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = LabDimens.GroupNameNotifGap)
                        .heightIn(min = LabDimens.InboxPreviewRowMinHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = inboxPreviewLine(item),
                        color = LabColors.White66,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(LabDimens.UnreadBadgeMinSize)
                                .clip(RoundedCornerShape(999.dp))
                                .background(accent().gradientBrush())
                                .padding(horizontal = LabDimens.UnreadBadgeHPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                                color = LabColors.White,
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
