package com.polli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import com.polli.domain.prefs.UiPreferences

@Composable
expect fun rememberScaledDensity(prefs: UiPreferences, uiScaleRevision: Int = 0): Density
