package com.polli.android.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/** Deterministic avatar tint from a seed string — polli profile_color.rs */
object ProfileColor {
    fun hue(seed: String): Float {
        var hash = 0
        for (c in seed) {
            hash = ((hash shl 5) - hash) + c.code
        }
        return (abs(hash) % 360).toFloat()
    }

    fun background(seed: String): Color {
        val h = hue(seed)
        return Color.hsl(h, 0.55f, 0.45f).copy(alpha = 0.24f)
    }

    fun text(seed: String): Color {
        val h = hue(seed)
        return Color.hsl(h, 0.55f, 0.53f)
    }
}
