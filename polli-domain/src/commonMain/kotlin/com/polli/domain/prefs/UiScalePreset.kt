package com.polli.domain.prefs

/** Nine-step UI scale — Small … Normal … Large with three steps between each anchor. */
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
    ;

    val anchor: UiScaleAnchor
        get() = when (ordinal) {
            in 0..3 -> UiScaleAnchor.Small
            in 4..6 -> UiScaleAnchor.Normal
            else -> UiScaleAnchor.Large
        }

    /** User-facing label — intermediate steps inherit their anchor name. */
    val displayLabel: String
        get() = label.ifBlank {
            when (anchor) {
                UiScaleAnchor.Small -> "Small"
                UiScaleAnchor.Normal -> "Normal"
                UiScaleAnchor.Large -> "Large"
            }
        }

    companion object {
        val default: UiScalePreset = Normal
    }
}

enum class UiScaleAnchor { Small, Normal, Large }
