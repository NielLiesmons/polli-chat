package com.polli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * Screen width in layout dp for the current [PolliTheme] UI scale.
 *
 * Divide by the app preset so lane gutters and max bubble width stay proportional to [PolliDimens]
 * when [LocalDensity] is scaled by [rememberScaledDensity].
 */
@Composable
expect fun layoutScreenWidthDp(): Dp
