package com.polli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

expect object AppInsets {
    @Composable
    fun statusBarTop(): Dp

    @Composable
    fun navigationBarBottom(): Dp
}
