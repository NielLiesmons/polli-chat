package com.polli.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.ui.home.formatHomeTabUnreadCount
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.PolliDimens
import com.polli.ui.theme.accent

/** Count badge pinned to the top-trailing edge of a tab pill (2dp inset unselected, 3dp selected). */
@Composable
fun TabCountBadge(
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val label = formatHomeTabUnreadCount(count) ?: return
    val inset = if (selected) 3.dp else 2.dp
    val tabHeight =
        if (selected) PolliDimens.TabButtonHeight else PolliDimens.TabButtonUnselectedHeight
    val badgeHeight = tabHeight - inset * 2
    val singleDigit = label.length == 1
    val shape = if (singleDigit) CircleShape else RoundedCornerShape(50)
    val bg = if (selected) accent().solid else PolliColors.White8

    Box(
        modifier =
            modifier
                .padding(top = inset, end = inset)
                .height(badgeHeight)
                .then(
                    if (singleDigit) {
                        Modifier.defaultMinSize(minWidth = badgeHeight, minHeight = badgeHeight)
                    } else {
                        Modifier.padding(horizontal = 6.dp)
                    },
                )
                .clip(shape)
                .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) PolliColors.White else PolliColors.White85,
            style =
                TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 11.sp,
                ),
        )
    }
}

/** Trailing inset for inline chrome (e.g. mail avatar stack) inside a tab pill. */
fun tabPillTrailingInset(selected: Boolean): Dp = if (selected) 3.dp else 2.dp
