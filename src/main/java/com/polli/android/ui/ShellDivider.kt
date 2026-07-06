package com.polli.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens

/**
 * Standard divider line — [PolliDimens.ShellDividerWidth] × [PolliColors.ShellDivider] (white8).
 * Pass [screenPad] = 0.dp for full-bleed dividers; default insets to [PolliDimens.HomeBarPadding].
 */
@Composable
fun ShellDivider(screenPad: Dp = PolliDimens.HomeBarPadding) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = screenPad)
            .height(PolliDimens.ShellDividerWidth)
            .background(PolliColors.ShellDivider),
    )
}
