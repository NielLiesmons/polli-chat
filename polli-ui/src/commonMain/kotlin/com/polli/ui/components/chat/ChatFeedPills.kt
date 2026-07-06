package com.polli.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.ui.components.ShellDivider
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.PolliDimens
import com.polli.ui.theme.accent

/** Centered day separator pill — white8 fill, white66 label. */
@Composable
fun ChatDayMarkerPill(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    top = PolliDimens.ChatFeedDayPillPadTop,
                    bottom = PolliDimens.ChatFeedDayPillPadBottom,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = PolliColors.White66,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(PolliDimens.ChatFeedPillRadius))
                    .background(PolliColors.White8)
                    .padding(
                        horizontal = PolliDimens.ChatFeedDayPillHPadding,
                        vertical = PolliDimens.ChatFeedDayPillVPadding,
                    ),
            style =
                TextStyle(
                    fontSize = PolliDimens.ChatFeedDayPillFontSize,
                    fontWeight = FontWeight.Medium,
                    lineHeight = PolliDimens.ChatFeedDayPillFontSize,
                ),
        )
    }
}

/** Unread boundary — accent 33% pill flanked by hairline dividers. */
@Composable
fun ChatNewMessagesPill(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = PolliDimens.ChatFeedNewPillPadV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShellDivider(screenPad = 0.dp)
        Box(
            modifier =
                Modifier
                    .padding(horizontal = PolliDimens.ChatFeedNewPillGap)
                    .clip(RoundedCornerShape(PolliDimens.ChatFeedPillRadius))
                    .background(accent().gradientBrush(0.33f))
                    .padding(
                        horizontal = PolliDimens.ChatFeedNewPillHPadding,
                        vertical = PolliDimens.ChatFeedNewPillVPadding,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = PolliColors.White,
                style =
                    TextStyle(
                        fontSize = PolliDimens.ChatFeedNewPillFontSize,
                        fontWeight = FontWeight.Medium,
                        lineHeight = PolliDimens.ChatFeedNewPillFontSize,
                    ),
            )
        }
        ShellDivider(screenPad = 0.dp)
    }
}
