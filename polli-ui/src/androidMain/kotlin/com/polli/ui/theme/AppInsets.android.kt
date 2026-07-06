package com.polli.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

actual object AppInsets {
    @Composable
    actual fun statusBarTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    @Composable
    actual fun navigationBarBottom(): Dp =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}
