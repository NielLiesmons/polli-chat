package com.polli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.polli.domain.prefs.UiPreferences

@Composable
actual fun rememberAccentPalette(prefs: UiPreferences, prefsRevision: Int): AccentPalette =
    remember(prefs.accentPreset, prefsRevision) {
        AccentThemes.palette(prefs.accentPreset)
    }
