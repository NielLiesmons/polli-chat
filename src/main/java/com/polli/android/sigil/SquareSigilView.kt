package com.polli.android.sigil

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.polli.core.sigil.MnsSigil
import kotlin.math.min
import kotlin.math.sqrt

/** Square-pixel reference renderer — matches the MNS HTML visualizer (1px gap). */
@Composable
fun SquareSigilView(
    value: ULong,
    modifier: Modifier = Modifier,
    onColor: Color = Color.White,
    offColor: Color = Color(0xFF0F0F0F),
) {
    val grid = remember(value) { MnsSigil.grid(value) }

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val diameter = min(size.width, size.height)
        val gridSide = diameter / sqrt(2f)
        val gap = 1f
        val cell = (gridSide - gap * (MnsSigil.COLS - 1)) / MnsSigil.COLS
        val ox = (size.width - gridSide) / 2f
        val oy = (size.height - gridSide) / 2f

        drawRect(offColor, topLeft = Offset(ox, oy), size = Size(gridSide, gridSide))

        for (r in 0 until MnsSigil.ROWS) {
            for (c in 0 until MnsSigil.COLS) {
                if (!grid[r][c]) continue
                val x = ox + c * (cell + gap)
                val y = oy + r * (cell + gap)
                drawRect(onColor, topLeft = Offset(x, y), size = Size(cell, cell))
            }
        }
    }
}
