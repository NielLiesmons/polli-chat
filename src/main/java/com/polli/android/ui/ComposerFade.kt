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

/** Text field edge fade — polli `.polli-chat-field-fade--scroll` */
fun Modifier.composerTextFadeMask(enabled: Boolean): Modifier {
    if (!enabled) return this
    return this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val fadePx = 8.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    fadePx / size.height to Color.Black,
                    (size.height - fadePx) / size.height to Color.Black,
                    1f to Color.Transparent,
                ),
                size = Size(size.width, size.height),
                blendMode = BlendMode.DstIn,
            )
        }
}
