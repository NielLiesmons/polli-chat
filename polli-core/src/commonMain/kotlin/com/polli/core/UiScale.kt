package com.polli.core

/** UI scale presets — polli preferences.rs parity. */
enum class UiScalePreset(val factor: Float, val label: String) {
    Small(0.925f, "Small"),
    Normal(1.0f, "Normal"),
    Large(1.075f, "Large"),
}

object UiScale {
    const val MIN_EFFECTIVE = 0.8f
    const val MAX_EFFECTIVE = 1.35f

    fun effective(preset: UiScalePreset, systemScale: Float, respectSystem: Boolean): Float {
        val base = if (respectSystem) preset.factor * systemScale else preset.factor
        return base.coerceIn(MIN_EFFECTIVE, MAX_EFFECTIVE)
    }
}
