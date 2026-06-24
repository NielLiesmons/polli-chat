package com.polli.domain.prefs

interface UiPreferences {
    val uiScalePreset: UiScalePreset
    val respectSystemScale: Boolean

    fun effectiveScale(systemFontScale: Float): Float {
        val base = uiScalePreset.factor
        return if (respectSystemScale) {
            (base * systemFontScale).coerceIn(0.8f, 1.35f)
        } else {
            base
        }
    }
}
