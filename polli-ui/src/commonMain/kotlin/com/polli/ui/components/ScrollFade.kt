package com.polli.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.PolliDimens

fun Modifier.scrollFadeMask(
    showTopFade: Boolean,
    bottomFadeHeight: Dp = PolliDimens.ScrollFadeBottom,
    topFadeHeight: Dp = PolliDimens.ScrollFadeTop,
): Modifier =
    composed {
        val density = LocalDensity.current
        val bottomPx = with(density) { bottomFadeHeight.toPx() }
        val topPx = with(density) { topFadeHeight.toPx() }
        alphaMaskModifier(showTopFade, topPx, bottomPx * 2.5f)
    }

private fun Modifier.alphaMaskModifier(
    showTopFade: Boolean,
    topFadePx: Float,
    bottomFadePx: Float,
): Modifier =
    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val h = size.height
            val w = size.width
            if (h <= 0f || w <= 0f) return@drawWithContent

            val stops =
                buildList {
                    if (showTopFade) {
                        add(0f to Color.Transparent)
                        add((20f / h).coerceIn(0f, 1f) to Color.Black.copy(alpha = 0.8f))
                        add((40f / h).coerceIn(0f, 1f) to Color.Black)
                    } else {
                        add(0f to Color.Black)
                    }
                    if (bottomFadePx > 0f) {
                        val bottomStart = ((h - bottomFadePx) / h).coerceIn(0f, 1f)
                        val bottomMid = ((h - bottomFadePx / 2f) / h).coerceIn(0f, 1f)
                        add(bottomStart to Color.Black)
                        add(bottomMid to Color.Black.copy(alpha = 0.8f))
                        add(1f to Color.Transparent)
                    } else {
                        add(1f to Color.Black)
                    }
                }

            drawRect(
                brush =
                    Brush.verticalGradient(
                        colorStops = stops.toTypedArray(),
                        startY = 0f,
                        endY = h,
                    ),
                size = Size(w, h),
                blendMode = BlendMode.DstIn,
            )
        }

@Composable
fun rememberLazyListShowTopFadeDerived(listState: LazyListState) =
    remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 4
        }
    }
