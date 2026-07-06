package com.polli.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Accent color tokens — one palette at a time (solid + gradient variants). */
data class AccentPalette(
    val solid: Color,
    val light: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
) {
    fun solid(alpha: Float): Color = solid.copy(alpha = alpha)

    fun light(alpha: Float): Color = light.copy(alpha = alpha)

    fun gradientBrush(alpha: Float = 1f): Brush = Brush.linearGradient(
        colors = listOf(
            gradientStart.copy(alpha = alpha),
            gradientEnd.copy(alpha = alpha),
        ),
    )
}

val LocalAccentPalette = staticCompositionLocalOf<AccentPalette> {
    error("AccentPalette not provided — wrap content in PolliTheme")
}

/** Current accent palette from [PolliTheme]. */
@Composable
fun accent(): AccentPalette = LocalAccentPalette.current
