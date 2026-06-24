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
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 9×9 MNS sigil — continuous blob shapes via corner wedges only.
 *
 * ON cell: solid square; carve corner wedges (outside inscribed circle) only where
 * both cardinals for that corner are OFF (exposed outer corner).
 *
 * OFF cell: corner wedge empty by default; fill when both cardinals for that corner
 * are ON (nook between neighbors). Drawn after ON cells so nooks stay visible.
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

        drawRect(offColor, topLeft = Offset(ox, oy), size = Size(gridSide, gridSide))

        // Pass 1 — ON cells: full square, round only exposed outer corners.
        for (r in 0 until MnsSigil.ROWS) {
            for (c in 0 until MnsSigil.COLS) {
                if (!grid[r][c]) continue
                val x = ox + c * cell
                val y = oy + r * cell
                drawOnCell(
                    x, y, cell, onColor, offColor,
                    n = cellOn(grid, r - 1, c),
                    e = cellOn(grid, r, c + 1),
                    south = cellOn(grid, r + 1, c),
                    w = cellOn(grid, r, c - 1),
                )
            }
        }

        // Pass 2 — OFF cells: nook corner wedges on top.
        for (r in 0 until MnsSigil.ROWS) {
            for (c in 0 until MnsSigil.COLS) {
                if (grid[r][c]) continue
                val x = ox + c * cell
                val y = oy + r * cell
                drawOffCell(
                    x, y, cell, onColor,
                    n = cellOn(grid, r - 1, c),
                    e = cellOn(grid, r, c + 1),
                    south = cellOn(grid, r + 1, c),
                    w = cellOn(grid, r, c - 1),
                )
            }
        }
    }
}

private fun cellOn(grid: Array<BooleanArray>, row: Int, col: Int): Boolean {
    if (row !in 0 until MnsSigil.ROWS || col !in 0 until MnsSigil.COLS) return false
    return grid[row][col]
}

private enum class Corner { TL, TR, BR, BL }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOnCell(
    x: Float,
    y: Float,
    cell: Float,
    onColor: Color,
    offColor: Color,
    n: Boolean,
    e: Boolean,
    south: Boolean,
    w: Boolean,
) {
    drawRect(onColor, topLeft = Offset(x, y), size = Size(cell, cell))

    val circle = inscribedCircle(x, y, cell)
    if (!n && !w) drawPath(cornerWedgePath(x, y, cell, circle, Corner.TL), offColor)
    if (!n && !e) drawPath(cornerWedgePath(x, y, cell, circle, Corner.TR), offColor)
    if (!south && !e) drawPath(cornerWedgePath(x, y, cell, circle, Corner.BR), offColor)
    if (!south && !w) drawPath(cornerWedgePath(x, y, cell, circle, Corner.BL), offColor)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOffCell(
    x: Float,
    y: Float,
    cell: Float,
    onColor: Color,
    n: Boolean,
    e: Boolean,
    south: Boolean,
    w: Boolean,
) {
    val circle = inscribedCircle(x, y, cell)
    if (n && w) drawPath(cornerWedgePath(x, y, cell, circle, Corner.TL), onColor)
    if (n && e) drawPath(cornerWedgePath(x, y, cell, circle, Corner.TR), onColor)
    if (south && e) drawPath(cornerWedgePath(x, y, cell, circle, Corner.BR), onColor)
    if (south && w) drawPath(cornerWedgePath(x, y, cell, circle, Corner.BL), onColor)
}

private fun inscribedCircle(x: Float, y: Float, cell: Float): Rect {
    val mx = x + cell / 2f
    val my = y + cell / 2f
    val r = cell / 2f
    return Rect(mx - r, my - r, mx + r, my + r)
}

/** Wedge outside the inscribed circle at the square corner. */
private fun cornerWedgePath(x: Float, y: Float, cell: Float, circle: Rect, corner: Corner): Path =
    Path().apply {
        val mx = x + cell / 2f
        val my = y + cell / 2f
        when (corner) {
            Corner.TL -> {
                moveTo(x, y)
                lineTo(x, my)
                arcTo(circle, 180f, -90f, false)
                close()
            }
            Corner.TR -> {
                moveTo(x + cell, y)
                lineTo(mx, y)
                arcTo(circle, 270f, -90f, false)
                close()
            }
            Corner.BR -> {
                moveTo(x + cell, y + cell)
                lineTo(x + cell, my)
                arcTo(circle, 0f, -90f, false)
                close()
            }
            Corner.BL -> {
                moveTo(x, y + cell)
                lineTo(mx, y + cell)
                arcTo(circle, 90f, -90f, false)
                close()
            }
        }
    }
