package com.polli.android.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.android.theme.PolliDimens

/**
 * Tracks the live bottom chrome (composer dock + safe/IME gap) from layout bounds so feed
 * padding and edge gradients stay aligned during keyboard open/close animations.
 *
 * Seeds an estimated composer inset on first layout so the feed does not jump when the dock
 * measures in (DC uses fixed list padding in XML from frame one).
 */
@Stable
class ComposerChromeLayoutState internal constructor(
    private val density: androidx.compose.ui.unit.Density,
    navigationBarBottom: Dp,
) {
    private var rootHeightPx by mutableFloatStateOf(0f)
    private var composerTopPx by mutableFloatStateOf(0f)
    private val estimatedDockPx =
        with(density) {
            (
                PolliDimens.ChatComposerMinHeight +
                    maxOf(PolliDimens.ChatComposerDockBottomMin, navigationBarBottom) +
                    PolliDimens.ChatComposerDockClearanceExtra
                ).toPx()
        }

    var dockHeightPx by mutableIntStateOf(0)
        private set

    fun onRootPositioned(coords: LayoutCoordinates) {
        rootHeightPx = coords.size.height.toFloat()
        if (composerTopPx <= 0f && rootHeightPx > 0f) {
            composerTopPx = (rootHeightPx - estimatedDockPx).coerceAtLeast(0f)
        }
    }

    fun onComposerPositioned(coords: LayoutCoordinates) {
        composerTopPx = coords.boundsInRoot().top
        dockHeightPx = coords.size.height
    }

    val bottomChromeInset: Dp
        get() = with(density) {
            if (rootHeightPx <= 0f) return@with 0.dp
            (rootHeightPx - composerTopPx).coerceAtLeast(0f).toDp()
        }

    val feedBottomPadding: Dp
        get() = bottomChromeInset + PolliDimens.ChatComposerDockClearanceExtra

    val dockHeight: Dp
        get() = with(density) { dockHeightPx.toDp() }
}

@Composable
fun rememberComposerChromeLayout(): ComposerChromeLayoutState {
    val density = LocalDensity.current
    val navigationBarBottom =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return remember(density, navigationBarBottom) {
        ComposerChromeLayoutState(density, navigationBarBottom)
    }
}
