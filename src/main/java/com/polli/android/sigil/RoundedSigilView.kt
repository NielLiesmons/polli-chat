package com.polli.android.sigil

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.polli.core.sigil.MnsSigil
import com.polli.core.sigil.SigilCornerRules
import kotlin.math.min

/** Whether the sigil canvas includes a solid backdrop or only the foreground shape. */
enum class SigilBackground {
    /** Filled square behind the sigil (Sigils tab preview). */
    Solid,
    /** Foreground shape only — parent supplies the circle background (e.g. Gray33 avatars). */
    Transparent,
}

/**
 * MNS sigil renderer. Foreground geometry is unioned into one [Path] and filled once so the
 * glyph reads as a single exportable shape (no per-cell grid seams).
 */
@Composable
fun RoundedSigilView(
    value: ULong,
    modifier: Modifier = Modifier,
    onColor: Color = Color.White,
    offColor: Color = Color(0xFF0F0F0F),
    background: SigilBackground = SigilBackground.Solid,
    /** Inset the sigil artwork from the canvas edge (e.g. 0.12f for circular avatars). */
    contentInsetFraction: Float = 0f,
) {
    val grid = remember(value) { MnsSigil.grid(value) }
    val silhouette = remember(grid, contentInsetFraction) {
        buildSigilSilhouette(grid, contentInsetFraction)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .drawWithCache {
                onDrawBehind {
                    drawRoundedSigil(
                        silhouette = silhouette,
                        onColor = onColor,
                        offColor = offColor,
                        background = background,
                    )
                }
            },
    )
}

private fun DrawScope.drawRoundedSigil(
    silhouette: Path,
    onColor: Color,
    offColor: Color,
    background: SigilBackground,
) {
    if (background == SigilBackground.Solid) {
        drawRect(offColor, size = size)
    }

    val side = min(size.width, size.height)
    val offsetX = (size.width - side) / 2f
    val offsetY = (size.height - side) / 2f
    translate(offsetX, offsetY) {
        scale(scaleX = side, scaleY = side, pivot = Offset.Zero) {
            drawPath(silhouette, onColor)
        }
    }
}

/** Unit-square (0..1) silhouette — scale to canvas at draw time. */
fun buildSigilSilhouette(
    grid: Array<BooleanArray>,
    contentInsetFraction: Float,
): Path {
    val inset = contentInsetFraction.coerceIn(0f, 0.4f)
    val drawSide = 1f - inset * 2f
    val cell = drawSide / MnsSigil.COLS
    val ox = inset
    val oy = inset

    var unified: Path? = null
    fun union(piece: Path) {
        unified = if (unified == null) {
            piece
        } else {
            Path.combine(PathOperation.Union, unified!!, piece)
        }
    }

    for (row in 0 until MnsSigil.ROWS) {
        for (col in 0 until MnsSigil.COLS) {
            if (!grid[row][col]) continue
            val x = ox + col * cell
            val y = oy + row * cell
            val n = cellOn(grid, row - 1, col)
            val e = cellOn(grid, row, col + 1)
            val s = cellOn(grid, row + 1, col)
            val w = cellOn(grid, row, col - 1)
            val nw = cellOn(grid, row - 1, col - 1)
            val ne = cellOn(grid, row - 1, col + 1)
            val sw = cellOn(grid, row + 1, col - 1)
            val se = cellOn(grid, row + 1, col + 1)
            val convex = convexCorners(n, e, s, w, nw, ne, sw, se)

            if (convex.isEmpty()) {
                union(cellCirclePath(x, y, cell))
                val circle = inscribedCircle(x, y, cell)
                cornerWedgePathIfNeeded(x, y, cell, circle, true, n, e, s, w, nw, ne, sw, se, SigilCornerRules.Corner.TL)?.let(::union)
                cornerWedgePathIfNeeded(x, y, cell, circle, true, n, e, s, w, nw, ne, sw, se, SigilCornerRules.Corner.TR)?.let(::union)
                cornerWedgePathIfNeeded(x, y, cell, circle, true, n, e, s, w, nw, ne, sw, se, SigilCornerRules.Corner.BR)?.let(::union)
                cornerWedgePathIfNeeded(x, y, cell, circle, true, n, e, s, w, nw, ne, sw, se, SigilCornerRules.Corner.BL)?.let(::union)
            }

            for (corner in convex) {
                union(convexCornerPath(x, y, cell, corner))
            }
        }
    }

    for (row in 0 until MnsSigil.ROWS) {
        for (col in 0 until MnsSigil.COLS) {
            if (grid[row][col]) continue
            val x = ox + col * cell
            val y = oy + row * cell
            val n = cellOn(grid, row - 1, col)
            val e = cellOn(grid, row, col + 1)
            val s = cellOn(grid, row + 1, col)
            val w = cellOn(grid, row, col - 1)
            val nw = cellOn(grid, row - 1, col - 1)
            val ne = cellOn(grid, row - 1, col + 1)
            val sw = cellOn(grid, row + 1, col - 1)
            val se = cellOn(grid, row + 1, col + 1)
            val circle = inscribedCircle(x, y, cell)
            cornerWedgePathIfNeeded(x, y, cell, circle, false, n, e, s, w, nw, ne, sw, se, SigilCornerRules.Corner.TL)?.let(::union)
            cornerWedgePathIfNeeded(x, y, cell, circle, false, n, e, s, w, nw, ne, sw, se, SigilCornerRules.Corner.TR)?.let(::union)
            cornerWedgePathIfNeeded(x, y, cell, circle, false, n, e, s, w, nw, ne, sw, se, SigilCornerRules.Corner.BR)?.let(::union)
            cornerWedgePathIfNeeded(x, y, cell, circle, false, n, e, s, w, nw, ne, sw, se, SigilCornerRules.Corner.BL)?.let(::union)
        }
    }

    return unified ?: Path()
}

private fun cellOn(grid: Array<BooleanArray>, row: Int, col: Int): Boolean {
    if (row !in 0 until MnsSigil.ROWS || col !in 0 until MnsSigil.COLS) return false
    return grid[row][col]
}

private fun convexCorners(
    n: Boolean,
    e: Boolean,
    s: Boolean,
    w: Boolean,
    nw: Boolean,
    ne: Boolean,
    sw: Boolean,
    se: Boolean,
): List<SigilCornerRules.Corner> =
    SigilCornerRules.Corner.entries.filter { corner ->
        val neighbors = SigilCornerRules.cornerNeighbors(corner, n, e, s, w, nw, ne, sw, se)
        SigilCornerRules.isConvexOuterCorner(true, corner, neighbors, n, e, s, w)
    }

private fun cellCirclePath(x: Float, y: Float, cell: Float): Path {
    val r = cell / 2f
    val mx = x + cell / 2f
    val my = y + cell / 2f
    return Path().apply { addOval(Rect(mx - r, my - r, mx + r, my + r)) }
}

private fun convexCornerPath(
    x: Float,
    y: Float,
    cell: Float,
    corner: SigilCornerRules.Corner,
): Path {
    val cellPath = Path().apply { addRect(Rect(x, y, x + cell, y + cell)) }
    val center = convexCircleCenter(x, y, cell, corner)
    val circlePath = Path().apply {
        addOval(Rect(center.x - cell, center.y - cell, center.x + cell, center.y + cell))
    }
    return Path.combine(PathOperation.Intersect, circlePath, cellPath)
}

private fun convexCircleCenter(
    x: Float,
    y: Float,
    cell: Float,
    corner: SigilCornerRules.Corner,
): Offset {
    val mx = x + cell / 2f
    val my = y + cell / 2f
    val h = cell / 2f
    return when (corner) {
        SigilCornerRules.Corner.TL -> Offset(mx + h, my + h)
        SigilCornerRules.Corner.TR -> Offset(mx - h, my + h)
        SigilCornerRules.Corner.BR -> Offset(mx - h, my - h)
        SigilCornerRules.Corner.BL -> Offset(mx + h, my - h)
    }
}

private fun inscribedCircle(x: Float, y: Float, cell: Float): Rect {
    val r = cell / 2f
    val mx = x + cell / 2f
    val my = y + cell / 2f
    return Rect(mx - r, my - r, mx + r, my + r)
}

private fun cornerWedgePathIfNeeded(
    x: Float,
    y: Float,
    cell: Float,
    circle: Rect,
    selfOn: Boolean,
    n: Boolean,
    e: Boolean,
    s: Boolean,
    w: Boolean,
    nw: Boolean,
    ne: Boolean,
    sw: Boolean,
    se: Boolean,
    corner: SigilCornerRules.Corner,
): Path? {
    val neighbors = SigilCornerRules.cornerNeighbors(corner, n, e, s, w, nw, ne, sw, se)
    if (!SigilCornerRules.fillCornerWedge(selfOn, corner, neighbors, n, e, s, w)) return null
    return cornerWedgePath(x, y, cell, circle, corner)
}

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
