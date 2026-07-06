package com.polli.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

enum class LabIconName {
    Search,
    Plus,
    ChevronLeft,
}

@Composable
fun LabIcon(
    icon: LabIconName,
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Canvas(modifier = modifier.size(size)) {
        val side = this.size.minDimension
        scale(side / 14f, side / 14f, pivot = Offset.Zero) {
            when (icon) {
                LabIconName.Plus -> {
                    val path =
                        PathParser()
                            .parsePathString(
                                "M6.25,1.25H7.75V6.25H12.75V7.75H7.75V12.75H6.25V7.75H1.25V6.25H6.25V1.25Z",
                            )
                            .toPath()
                    drawPath(path, color)
                }
                LabIconName.ChevronLeft -> {
                    val path =
                        PathParser()
                            .parsePathString(
                                "M6.85,13.15L1.15,7.45L6.85,1.75L7.85,2.75L3.15,7.45L7.85,12.15L6.85,13.15Z",
                            )
                            .toPath()
                    drawPath(path, color)
                }
                LabIconName.Search -> {
                    drawCircle(
                        color = color,
                        radius = 4.2f,
                        center = Offset(6f, 6f),
                        style = Stroke(width = 1.6f),
                    )
                    drawLine(
                        color = color,
                        start = Offset(9.2f, 9.2f),
                        end = Offset(12.5f, 12.5f),
                        strokeWidth = 1.6f,
                    )
                }
            }
        }
    }
}
