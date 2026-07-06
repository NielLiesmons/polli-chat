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
import com.polli.ui.theme.LabColors
import com.polli.ui.theme.LabDimens
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
                    top = LabDimens.ChatFeedDayPillPadTop,
                    bottom = LabDimens.ChatFeedDayPillPadBottom,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = LabColors.White66,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(LabDimens.ChatFeedPillRadius))
                    .background(LabColors.White8)
                    .padding(
                        horizontal = LabDimens.ChatFeedDayPillHPadding,
                        vertical = LabDimens.ChatFeedDayPillVPadding,
                    ),
            style =
                TextStyle(
                    fontSize = LabDimens.ChatFeedDayPillFontSize,
                    fontWeight = FontWeight.Medium,
                    lineHeight = LabDimens.ChatFeedDayPillFontSize,
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
                .padding(vertical = LabDimens.ChatFeedNewPillPadV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShellDivider(screenPad = 0.dp)
        Box(
            modifier =
                Modifier
                    .padding(horizontal = LabDimens.ChatFeedNewPillGap)
                    .clip(RoundedCornerShape(LabDimens.ChatFeedPillRadius))
                    .background(accent().gradientBrush(0.33f))
                    .padding(
                        horizontal = LabDimens.ChatFeedNewPillHPadding,
                        vertical = LabDimens.ChatFeedNewPillVPadding,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = LabColors.White,
                style =
                    TextStyle(
                        fontSize = LabDimens.ChatFeedNewPillFontSize,
                        fontWeight = FontWeight.Medium,
                        lineHeight = LabDimens.ChatFeedNewPillFontSize,
                    ),
            )
        }
        ShellDivider(screenPad = 0.dp)
    }
}
