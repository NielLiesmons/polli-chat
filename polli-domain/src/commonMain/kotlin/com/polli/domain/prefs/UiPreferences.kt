package com.polli.domain.prefs

interface UiPreferences {
    val uiScalePreset: UiScalePreset
    val respectSystemScale: Boolean
    val accentPreset: AccentPreset

    fun effectiveScale(systemFontScale: Float): Float = uiScalePreset.factor
}
