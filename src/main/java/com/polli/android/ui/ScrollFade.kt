package com.polli.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens

/**
 * Alpha masks — polli `.polli-scroll-mask-host` (home / non-chat lists).
 * Uses [BlendMode.DstIn] so content fades under overlaid chrome.
 */
fun Modifier.scrollFadeMask(
    showTopFade: Boolean,
    bottomFadeHeight: Dp = LabDimens.ScrollFadeBottom,
    topFadeHeight: Dp = LabDimens.ScrollFadeTop,
): Modifier = composed {
    val density = LocalDensity.current
    val bottomPx = with(density) { bottomFadeHeight.toPx() }
    val topPx = with(density) { topFadeHeight.toPx() }
    alphaMaskModifier(showTopFade, topPx, bottomPx * 2.5f, groupHeaderPx = 0f)
}

/** DstIn dissolve for home search panel — fades under the fixed search input row only. */
fun Modifier.homeSearchPanelScrollFade(
    showTopFade: Boolean = true,
): Modifier = composed {
    val density = LocalDensity.current
    val topPx = with(density) { LabDimens.HomeSearchPanelTopFade.toPx() }
    alphaMaskModifier(showTopFade, topPx, bottomFadePx = 0f, groupHeaderPx = 0f)
}

/**
 * Three-stop edge gradients over the chat feed (pass-through touches).
 *
 * Bottom fade height = safe area + composer bar + 8dp; midpoint at 75% black, inner edge transparent.
 * Top fade is the reverse (from the top screen edge).
 */
@Composable
fun ChatFeedEdgeGradients(
    modifier: Modifier = Modifier,
    topChromeClearance: Dp = 0.dp,
    showTopFade: Boolean = false,
    bottomChromeInset: Dp = 0.dp,
) {
    val density = LocalDensity.current
    val navBottom = AppInsets.navigationBarBottom()
    val bottomFadeHeight = bottomChromeInset.coerceAtLeast(
        navBottom + LabDimens.ChatComposerMinHeight + 8.dp,
    ) + LabDimens.ChatFeedBottomFadeExtend
    val topFadeHeight = when {
        topChromeClearance > 0.dp ->
            topChromeClearance + LabDimens.ChatFeedTopFadeExtend
        showTopFade ->
            LabDimens.ScrollFadeTop + LabDimens.ChatFeedTopFadeExtend
        else -> 0.dp
    }

    Box(
        modifier = modifier
            .pointerInteropFilter { false }
            .drawBehind {
                val h = size.height
                val w = size.width
                if (h <= 0f || w <= 0f) return@drawBehind

                val bottomPx = with(density) { bottomFadeHeight.toPx() }
                if (bottomPx > 0f) {
                    val bottomTop = h - bottomPx
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.5f to LabColors.Black.copy(alpha = 0.75f),
                                1f to LabColors.Black,
                            ),
                            startY = bottomTop,
                            endY = h,
                        ),
                        topLeft = Offset(0f, bottomTop),
                        size = Size(w, bottomPx),
                    )
                }

                val topPx = with(density) { topFadeHeight.toPx() }
                if (topPx > 0f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to LabColors.Black,
                                0.5f to LabColors.Black.copy(alpha = 0.75f),
                                1f to Color.Transparent,
                            ),
                            startY = 0f,
                            endY = topPx,
                        ),
                        size = Size(w, topPx),
                    )
                }
            },
    )
}

private fun Modifier.alphaMaskModifier(
    showTopFade: Boolean,
    topFadePx: Float,
    bottomFadePx: Float,
    groupHeaderPx: Float,
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = size.height
        val w = size.width
        if (h <= 0f || w <= 0f) return@drawWithContent

        val stops = buildList {
            if (groupHeaderPx > 0f) {
                add(0f to Color.Transparent)
                add(((groupHeaderPx + 12f) / h).coerceIn(0f, 1f) to Color.Black.copy(alpha = 0.8f))
                add(((groupHeaderPx + 24f) / h).coerceIn(0f, 1f) to Color.Black)
            } else if (showTopFade) {
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
            brush = Brush.verticalGradient(
                colorStops = stops.toTypedArray(),
                startY = 0f,
                endY = h,
            ),
            size = Size(w, h),
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
fun rememberLazyListShowTopFadeDerived(listState: LazyListState) = remember {
    derivedStateOf {
        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 4
    }
}
