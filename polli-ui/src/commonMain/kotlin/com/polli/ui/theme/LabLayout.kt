package com.polli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Screen width in layout dp for the current [LabTheme] UI scale.
 *
 * Divide by the app preset so lane gutters and max bubble width stay proportional to [LabDimens]
 * when [LocalDensity] is scaled by [rememberScaledDensity].
 */
@Composable
fun layoutScreenWidthDp(): Dp {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val scale = LocalUiPreferences.current.uiScalePreset.factor
    return (screenWidthDp / scale).dp
}
