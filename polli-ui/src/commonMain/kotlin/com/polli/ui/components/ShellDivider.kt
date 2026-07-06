package com.polli.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.PolliDimens

/** Hairline divider — full width with optional horizontal inset. */
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
