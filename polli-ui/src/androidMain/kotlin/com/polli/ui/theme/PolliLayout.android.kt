package com.polli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
actual fun layoutScreenWidthDp(): Dp {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val scale = LocalUiPreferences.current.uiScalePreset.factor
    return (screenWidthDp / scale).dp
}
