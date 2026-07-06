package com.polli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.polli.domain.prefs.UiPreferences

@Composable
actual fun rememberScaledDensity(prefs: UiPreferences, uiScaleRevision: Int): Density {
    val base = LocalDensity.current
    val factor = remember(uiScaleRevision, prefs.uiScalePreset) {
        prefs.effectiveScale(1f)
    }
    return Density(base.density * factor, factor)
}
