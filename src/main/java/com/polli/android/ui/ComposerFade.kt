package com.polli.android.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/** Text field edge fade — only at scroll edges with clipped overflow (zapstore modal pattern). */
fun Modifier.composerTextFadeMask(
    fadeTop: Boolean = false,
    fadeBottom: Boolean = false,
): Modifier {
    if (!fadeTop && !fadeBottom) return this
    return this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val fadePx = 8.dp.toPx()
            val h = size.height
            if (h <= 0f) return@drawWithContent
            val fadeStop = (fadePx / h).coerceIn(0f, 0.48f)

            val stops = when {
                fadeTop && fadeBottom -> arrayOf(
                    0f to Color.Transparent,
                    fadeStop to Color.Black,
                    (1f - fadeStop) to Color.Black,
                    1f to Color.Transparent,
                )
                fadeTop -> arrayOf(
                    0f to Color.Transparent,
                    fadeStop to Color.Black,
                    1f to Color.Black,
                )
                fadeBottom -> arrayOf(
                    0f to Color.Black,
                    (1f - fadeStop) to Color.Black,
                    1f to Color.Transparent,
                )
                else -> return@drawWithContent
            }

            drawRect(
                brush = Brush.verticalGradient(colorStops = stops, startY = 0f, endY = h),
                size = Size(size.width, h),
                blendMode = BlendMode.DstIn,
            )
        }
}
