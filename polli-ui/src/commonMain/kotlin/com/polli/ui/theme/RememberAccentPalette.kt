package com.polli.ui.theme

import androidx.compose.runtime.Composable
import com.polli.domain.prefs.UiPreferences

/** Resolves the active accent palette, re-reading [UiPreferences] when the host resumes. */
@Composable
expect fun rememberAccentPalette(prefs: UiPreferences, prefsRevision: Int = 0): AccentPalette
