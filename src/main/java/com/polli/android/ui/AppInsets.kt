package com.polli.android.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.android.theme.PolliDimens

object AppInsets {
    @Composable
    fun statusBarTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    @Composable
    fun navigationBarBottom(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    @Composable
    fun chatComposerClearance(): Dp {
        val bottom = navigationBarBottom()
        return PolliDimens.ChatComposerMinHeight +
            maxOf(PolliDimens.ChatComposerDockBottomMin, bottom) +
            PolliDimens.ChatComposerDockClearanceExtra
    }
}
