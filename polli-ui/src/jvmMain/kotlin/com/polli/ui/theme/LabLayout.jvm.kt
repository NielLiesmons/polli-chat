package com.polli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
actual fun layoutScreenWidthDp(): Dp {
    val scale = LocalUiPreferences.current.uiScalePreset.factor
    return (960f / scale).dp
}
