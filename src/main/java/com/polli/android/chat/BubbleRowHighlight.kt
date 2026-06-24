package com.polli.android.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.polli.android.theme.LabColors

private val HighlightBandColor = LabColors.White8

/**
 * Full-width white band behind a bubble row — draws edge-to-edge without changing layout or alignment.
 */
fun Modifier.messageRowHighlight(visible: Boolean): Modifier = composed {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (visible) 120 else 500),
        label = "rowHighlight",
    )
    if (alpha <= 0.01f) return@composed this
    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    var rootX by remember { mutableFloatStateOf(0f) }
    onGloballyPositioned { rootX = it.positionInRoot().x }
        .drawBehind {
            drawRect(
                color = HighlightBandColor.copy(alpha = HighlightBandColor.alpha * alpha),
                topLeft = Offset(-rootX, 0f),
                size = Size(screenWidthPx, size.height),
            )
        }
}
