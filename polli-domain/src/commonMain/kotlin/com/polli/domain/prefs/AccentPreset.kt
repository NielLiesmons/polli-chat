package com.polli.domain.prefs

/** User-selectable accent palette — solid + gradient tokens only (no gold/rouge). */
enum class AccentPreset(val id: String, val label: String) {
    Default("default", "Blurple"),
    Pink("pink", "Pink"),
    Ocean("ocean", "Ocean"),
    Blue("blue", "Blue"),
    Purple("purple", "Purple"),
    ;

    companion object {
        fun fromId(id: String): AccentPreset =
            entries.find { it.id == id } ?: Default
    }
}
