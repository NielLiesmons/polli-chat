package com.polli.android.theme

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import java.math.BigInteger
import kotlin.math.abs

/** Deterministic profile colors — port of polli/profile_color.rs + zapstore color.dart */
object ProfileColors {
    fun stringToColor(value: String): Color {
        val normalized = value.trim().uppercase()
        if (normalized.isEmpty()) return Color(0xFF808080)
        // u128 mod 360 — matches polli (Long overflows on email-length seeds).
        var acc = BigInteger.ZERO
        var pow256 = BigInteger.ONE
        val mod360 = BigInteger.valueOf(360)
        for (ch in normalized) {
            acc = (acc + BigInteger.valueOf(ch.code.toLong()) * pow256).mod(mod360)
            pow256 = (pow256 * BigInteger.valueOf(256)).mod(mod360)
        }
        return hsvToColor(acc.toInt(), s = 0.70f, vMid = 0.70f)
    }

    fun profileTextColor(base: Color): Color {
        val factor = 1.08f
        return Color(
            red = (base.red * factor).coerceIn(0f, 1f),
            green = (base.green * factor).coerceIn(0f, 1f),
            blue = (base.blue * factor).coerceIn(0f, 1f),
            alpha = base.alpha,
        )
    }

    /** Polli seeds author colors from contact addr when available, else numeric id / name. */
    fun authorNameColor(colorSeed: String): Color {
        if (colorSeed.isBlank()) return profileTextColor(stringToColor(""))
        return profileTextColor(stringToColor(colorSeed))
    }

    /** DC contact.getColor() returns 0x00RRGGBB — not Android ARGB. */
    fun fromDcRgb(rgb: Int): Color {
        if (rgb == 0) return Color.Gray
        return Color(
            AndroidColor.red(rgb),
            AndroidColor.green(rgb),
            AndroidColor.blue(rgb),
        )
    }

    private fun hsvToColor(hue: Int, s: Float, vMid: Float): Color {
        val v = when (hue) {
            in 32..204 -> vMid
            in 216..273 -> 0.96f
            else -> 0.90f
        }
        val h = hue / 60f
        val c = v * s
        val x = c * (1f - abs((h % 2f) - 1f))
        val m = v - c
        val (r1, g1, b1) = when {
            h < 1f -> Triple(c, x, 0f)
            h < 2f -> Triple(x, c, 0f)
            h < 3f -> Triple(0f, c, x)
            h < 4f -> Triple(0f, x, c)
            h < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(r1 + m, g1 + m, b1 + m)
    }
}
