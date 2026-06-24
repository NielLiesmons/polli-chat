package com.polli.android.sigil

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.polli.core.sigil.MnsSigil
import com.polli.core.sigil.SigilCornerRules
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Each cell = inscribed circle (filled when ON, empty when OFF)
 * + four corner wedges outside the circle (the leftover square corners).
 */
@Composable
fun RoundedSigilView(
    value: ULong,
    modifier: Modifier = Modifier,
    onColor: Color = Color.White,
    offColor: Color = Color(0xFF0F0F0F),
) {
    val grid = remember(value) { MnsSigil.grid(value) }

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val diameter = min(size.width, size.height)
        val gridSide = diameter / sqrt(2f)
        val cell = gridSide / MnsSigil.COLS
        val ox = (size.width - gridSide) / 2f
        val oy = (size.height - gridSide) / 2f
        val r = cell / 2f

        drawRect(offColor, topLeft = Offset(ox, oy), size = Size(gridSide, gridSide))

        // Pass 1 — circles
        for (row in 0 until MnsSigil.ROWS) {
            for (col in 0 until MnsSigil.COLS) {
                if (!grid[row][col]) continue
                val x = ox + col * cell
                val y = oy + row * cell
                drawCircle(onColor, radius = r, center = Offset(x + r, y + r))
            }
        }

        // Pass 2 — corner wedges (on top)
        for (row in 0 until MnsSigil.ROWS) {
            for (col in 0 until MnsSigil.COLS) {
                val x = ox + col * cell
                val y = oy + row * cell
                val on = grid[row][col]
                val n = cellOn(grid, row - 1, col)
                val e = cellOn(grid, row, col + 1)
                val s = cellOn(grid, row + 1, col)
                val w = cellOn(grid, row, col - 1)
                val circle = inscribedCircle(x, y, cell)

                drawCornerWedgeIfNeeded(x, y, cell, circle, onColor, on, n, w, SigilCornerRules.Corner.TL)
                drawCornerWedgeIfNeeded(x, y, cell, circle, onColor, on, n, e, SigilCornerRules.Corner.TR)
                drawCornerWedgeIfNeeded(x, y, cell, circle, onColor, on, s, e, SigilCornerRules.Corner.BR)
                drawCornerWedgeIfNeeded(x, y, cell, circle, onColor, on, s, w, SigilCornerRules.Corner.BL)
            }
        }
    }
}

private fun cellOn(grid: Array<BooleanArray>, row: Int, col: Int): Boolean {
    if (row !in 0 until MnsSigil.ROWS || col !in 0 until MnsSigil.COLS) return false
    return grid[row][col]
}

private fun inscribedCircle(x: Float, y: Float, cell: Float): Rect {
    val r = cell / 2f
    val mx = x + r
    val my = y + r
    return Rect(mx - r, my - r, mx + r, my + r)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerWedgeIfNeeded(
    x: Float,
    y: Float,
    cell: Float,
    circle: Rect,
    color: Color,
    selfOn: Boolean,
    neighborA: Boolean,
    neighborB: Boolean,
    corner: SigilCornerRules.Corner,
) {
    if (!SigilCornerRules.fillCornerWedge(selfOn, neighborA, neighborB)) return
    drawPath(cornerWedgePath(x, y, cell, circle, corner), color)
}

/**
 * Corner wedge outside the inscribed circle.
 * Arc runs clockwise from the first edge midpoint to the second (sweep = +90°).
 */
private fun cornerWedgePath(
    x: Float,
    y: Float,
    cell: Float,
    circle: Rect,
    corner: SigilCornerRules.Corner,
): Path = Path().apply {
    val mx = x + cell / 2f
    val my = y + cell / 2f
    when (corner) {
        SigilCornerRules.Corner.TL -> {
            moveTo(x, y)
            lineTo(x, my)
            arcTo(circle, 180f, 90f, false)
            close()
        }
        SigilCornerRules.Corner.TR -> {
            moveTo(x + cell, y)
            lineTo(mx, y)
            arcTo(circle, 270f, 90f, false)
            close()
        }
        SigilCornerRules.Corner.BR -> {
            moveTo(x + cell, y + cell)
            lineTo(x + cell, my)
            arcTo(circle, 0f, 90f, false)
            close()
        }
        SigilCornerRules.Corner.BL -> {
            moveTo(x, y + cell)
            lineTo(mx, y + cell)
            arcTo(circle, 90f, 90f, false)
            close()
        }
    }
}
