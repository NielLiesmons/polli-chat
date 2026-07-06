package com.polli.desktop

import com.polli.domain.prefs.AccentPreset
import com.polli.domain.prefs.UiPreferences
import com.polli.domain.prefs.UiScalePreset

class DesktopUiPreferences : UiPreferences {
    override val uiScalePreset: UiScalePreset = UiScalePreset.default
    override val respectSystemScale: Boolean = false
    override val accentPreset: AccentPreset = AccentPreset.Default
}
