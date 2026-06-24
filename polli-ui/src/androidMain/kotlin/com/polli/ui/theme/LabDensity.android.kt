package com.polli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.polli.domain.prefs.UiPreferences

@Composable
actual fun rememberScaledDensity(prefs: UiPreferences, uiScaleRevision: Int): Density {
    val base = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by mutableIntStateOf(0)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val systemScale = LocalConfiguration.current.fontScale
    val factor = remember(uiScaleRevision, resumeTick, systemScale, prefs.uiScalePreset, prefs.respectSystemScale) {
        prefs.effectiveScale(systemScale)
    }
    return Density(base.density * factor, base.fontScale * factor)
}
