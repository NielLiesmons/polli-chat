package com.polli.core

/** UI scale presets — mirrors [com.polli.domain.prefs.UiScalePreset]. */
enum class UiScalePreset(val factor: Float, val label: String) {
    Small(0.85f, "Small"),
    Step2(0.89f, ""),
    Step3(0.93f, ""),
    Step4(0.97f, ""),
    Normal(1.0f, "Normal"),
    Step6(1.03f, ""),
    Step7(1.06f, ""),
    Step8(1.09f, ""),
    Large(1.12f, "Large"),
}

object UiScale {
    const val MIN_EFFECTIVE = 0.8f
    const val MAX_EFFECTIVE = 1.35f

    fun effective(preset: UiScalePreset, systemScale: Float = 1f, respectSystem: Boolean = false): Float {
        return preset.factor.coerceIn(MIN_EFFECTIVE, MAX_EFFECTIVE)
    }
}
