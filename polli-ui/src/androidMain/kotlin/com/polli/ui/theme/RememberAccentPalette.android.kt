package com.polli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.polli.domain.prefs.UiPreferences

@Composable
actual fun rememberAccentPalette(prefs: UiPreferences, prefsRevision: Int): AccentPalette {
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by mutableIntStateOf(0)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return remember(prefs.accentPreset, prefsRevision, resumeTick) {
        AccentThemes.palette(prefs.accentPreset)
    }
}
