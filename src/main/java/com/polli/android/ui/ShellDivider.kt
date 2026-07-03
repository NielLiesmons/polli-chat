package com.polli.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens

/**
 * Standard divider line — [LabDimens.ShellDividerWidth] × [LabColors.ShellDivider] (white8).
 * Pass [screenPad] = 0.dp for full-bleed dividers; default insets to [LabDimens.HomeBarPadding].
 */
@Composable
fun ShellDivider(screenPad: Dp = LabDimens.HomeBarPadding) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = screenPad)
            .height(LabDimens.ShellDividerWidth)
            .background(LabColors.ShellDivider),
    )
}
