package com.polli.domain.prefs

enum class UiScalePreset(val factor: Float, val label: String) {
    Small(0.925f, "Small"),
    Normal(1.0f, "Normal"),
    Large(1.075f, "Large"),
}
