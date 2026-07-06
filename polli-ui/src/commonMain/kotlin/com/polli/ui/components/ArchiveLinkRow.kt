package com.polli.ui.components

import androidx.compose.foundation.background
import com.polli.ui.components.polliClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.PolliDimens
import com.polli.ui.theme.accent

@Composable
fun ArchiveLinkRow(
    label: String,
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .polliClickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(PolliDimens.AvatarSize)
                .clip(CircleShape)
                .background(PolliColors.Gray66),
            contentAlignment = Alignment.Center,
        ) {
            Text("⌂", color = PolliColors.White66, fontSize = 20.sp)
        }
        Text(
            text = label,
            color = PolliColors.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .height(PolliDimens.UnreadBadgeMinSize)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent().gradientBrush())
                    .padding(horizontal = PolliDimens.UnreadBadgeHPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    color = PolliColors.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
