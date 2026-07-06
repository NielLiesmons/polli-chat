package com.polli.android.chat

import com.polli.domain.model.chat.OutgoingState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.icons.PolliIcon
import com.polli.android.icons.PolliIconName
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens

/** Inside bubble — incoming author + timestamp row (polli message_bubble.rs). */
@Composable
fun IncomingBubbleHeader(
    authorName: String,
    authorColor: Color,
    timestamp: String,
    isEdited: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .width(IntrinsicSize.Max)
            .padding(horizontal = PolliDimens.ChatBubbleInsetH)
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = authorName,
            color = authorColor,
            fontSize = 13.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEdited) {
                Text(
                    "Edited",
                    color = PolliColors.White33,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                )
            }
            Text(
                timestamp,
                color = PolliColors.White33,
                fontSize = 11.sp,
                lineHeight = 11.sp,
            )
        }
    }
}

/** Inside bubble — outgoing timestamp / edited / receipts (polli OutgoingBubbleMeta). */
@Composable
fun OutgoingBubbleMetaRow(
    timestamp: String,
    state: OutgoingState?,
    isEdited: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .width(IntrinsicSize.Max)
            .padding(
                horizontal = PolliDimens.ChatBubbleInsetH,
                vertical = PolliDimens.ChatBubbleMetaRowPaddingV,
            )
            .offset(y = PolliDimens.ChatBubbleMetaRowMarginTop),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEdited) {
                Text("Edited", color = PolliColors.White33, fontSize = 11.sp, lineHeight = 11.sp)
            }
            Text(timestamp, color = PolliColors.White66, fontSize = 11.sp, lineHeight = 11.sp)
            when (state) {
                OutgoingState.Sending -> Text(
                    "…",
                    color = PolliColors.White66.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                )
                OutgoingState.Sent -> PolliIcon(
                    PolliIconName.Check,
                    11.dp,
                    PolliColors.White66,
                )
                OutgoingState.Read -> Row {
                    PolliIcon(PolliIconName.Check, 11.dp, PolliColors.White66)
                    PolliIcon(
                        PolliIconName.Check,
                        11.dp,
                        PolliColors.White66,
                        modifier = Modifier.offset(x = (-3).dp),
                    )
                }
                OutgoingState.Failed -> Text(
                    "!",
                    color = PolliColors.Destructive,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                )
                null -> Unit
            }
        }
    }
}
