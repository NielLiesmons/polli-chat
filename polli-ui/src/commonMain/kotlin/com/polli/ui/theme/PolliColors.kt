package com.polli.ui.theme

import androidx.compose.ui.graphics.Color

/** Neutral Polli design tokens — accent colors live in [AccentPalette] / [accent]. */
object PolliColors {
    val Black = Color(0xFF121212)
    val White = Color(0xFFFFFFFF)
    val White66 = Color(0xA8FFFFFF)
    val White85 = Color(0xD9FFFFFF)
    val White33 = Color(0x54FFFFFF)
    val White16 = Color(0x29FFFFFF)
    val White11 = Color(0x1CFFFFFF)
    val White8 = Color(0x14FFFFFF)
    val White4 = Color(0x0AFFFFFF)
    val ShellBorder = White11
    /** Full-width divider lines ([ShellDivider]). */
    val ShellDivider = White8
    val Gray = Color(0xFF242424)
    val Gray66 = Color(0xA8333333)
    val Gray33 = Color(0x54333333)
    val Black33 = Color(0x54000000)
    val Black16 = Color(0x29000000)
    val Black8 = Color(0x14000000)
    /** Zapstore rouge — destructive actions (theme-independent). */
    val Rouge = Color(0xFFFF4778)
    val RougeEnd = Color(0xFFFF005E)
    val Rouge66 = Color(0xA8FF4778)
    val Destructive = Rouge
}
