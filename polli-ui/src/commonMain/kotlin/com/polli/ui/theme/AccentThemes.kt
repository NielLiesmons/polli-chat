package com.polli.ui.theme

import androidx.compose.ui.graphics.Color
import com.polli.domain.prefs.AccentPreset

/**
 * Accent presets ported from zapchat [color_themes.dart] / zaplab_design blurple tokens.
 * Only accent (formerly blurple) colors — no gold or rouge.
 */
object AccentThemes {
    fun palette(preset: AccentPreset): AccentPalette = when (preset) {
        AccentPreset.Default -> AccentPalette(
            solid = Color(0xFF5C5AFE),
            light = Color(0xFF8483FE),
            gradientStart = Color(0xFF636AFF),
            gradientEnd = Color(0xFF5445FF),
        )
        AccentPreset.Pink -> AccentPalette(
            solid = Color(0xFFE33F8A),
            light = Color(0xFFFF58A4),
            gradientStart = Color(0xFFE340A2),
            gradientEnd = Color(0xFFD73E6C),
        )
        AccentPreset.Ocean -> AccentPalette(
            solid = Color(0xFF128D8C),
            light = Color(0xFF128D8C),
            gradientStart = Color(0xFF00956B),
            gradientEnd = Color(0xFF1F749C),
        )
        AccentPreset.Blue -> AccentPalette(
            solid = Color(0xFF4434FF),
            light = Color(0xFF6174FF),
            gradientStart = Color(0xFF3C4FFF),
            gradientEnd = Color(0xFF3C2BF7),
        )
        AccentPreset.Purple -> AccentPalette(
            solid = Color(0xFF7D3DE9),
            light = Color(0xFF9657FF),
            gradientStart = Color(0xFF8A3DE9),
            gradientEnd = Color(0xFF713DE9),
        )
    }
}
