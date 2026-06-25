package com.polli.android.chat

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
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens

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
            .padding(horizontal = LabDimens.ChatBubbleInsetH)
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = authorName,
            color = authorColor,
            fontSize = 13.sp,
            lineHeight = 19.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 8.dp)
                .alignByBaseline(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (isEdited) {
                Text(
                    "Edited",
                    color = LabColors.White33,
                    fontSize = 11.sp,
                    lineHeight = 16.5.sp,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Text(
                timestamp,
                color = LabColors.White33,
                fontSize = 11.sp,
                lineHeight = 16.5.sp,
                modifier = Modifier.alignByBaseline(),
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
                horizontal = LabDimens.ChatBubbleInsetH,
                vertical = LabDimens.ChatBubbleMetaRowPaddingV,
            )
            .offset(y = LabDimens.ChatBubbleMetaRowMarginTop),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEdited) {
                Text("Edited", color = LabColors.White33, fontSize = 11.sp, lineHeight = 11.sp)
            }
            Text(timestamp, color = LabColors.White66, fontSize = 11.sp, lineHeight = 11.sp)
            when (state) {
                OutgoingState.Sending -> Text(
                    "…",
                    color = LabColors.White66.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                )
                OutgoingState.Sent -> LabIcon(
                    LabIconName.Check,
                    11.dp,
                    LabColors.White66,
                )
                OutgoingState.Read -> Row {
                    LabIcon(LabIconName.Check, 11.dp, LabColors.White66)
                    LabIcon(
                        LabIconName.Check,
                        11.dp,
                        LabColors.White66,
                        modifier = Modifier.offset(x = (-3).dp),
                    )
                }
                OutgoingState.Failed -> Text(
                    "!",
                    color = LabColors.Destructive,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                )
                null -> Unit
            }
        }
    }
}
