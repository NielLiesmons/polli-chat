package com.polli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Screen width in layout dp for the current [LabTheme] UI scale.
 *
 * [LocalConfiguration] reports the physical screen in dp, but [LabTheme] multiplies [androidx.compose.ui.unit.Density]
 * by the UI scale factor — so raw `screenWidthDp.dp` grows with scale while the device width in px stays fixed.
 * That makes lane gutters shrink at large scale and grow at small scale. Divide by the scale factor so padding and
 * max bubble width stay proportional to other LabDimens.
 */
@Composable
fun layoutScreenWidthDp(): Dp {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val scale = LocalUiPreferences.current.uiScalePreset.factor
    return (screenWidthDp / scale).dp
}
