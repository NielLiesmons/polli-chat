package com.polli.android.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.ui.FrostedCircleButton
import dev.chrisbanes.haze.HazeState
import kotlin.math.abs

/** Mirrors [org.thoughtcrime.securesms.ConversationFragment.SCROLL_ANIMATION_THRESHOLD]. */
private const val SCROLL_ANIMATION_THRESHOLD = 50

/** Mirrors [ConversationFragment.ConversationScrollListener.isAtZoomScrollHeight]. */
private const val SHOW_FAB_ITEM_THRESHOLD = 4

fun LazyListState.isAtChatBottom(): Boolean {
    if (layoutInfo.totalItemsCount == 0) return true
    if (firstVisibleItemIndex != 0) return false
    val first = layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 } ?: return true
    return first.offset >= 0
}

fun LazyListState.shouldShowScrollToBottomFab(): Boolean {
    if (layoutInfo.totalItemsCount == 0) return false
    if (isAtChatBottom()) return false
    return firstVisibleItemIndex >= SHOW_FAB_ITEM_THRESHOLD
}

suspend fun LazyListState.scrollToChatBottom(animated: Boolean) {
    if (layoutInfo.totalItemsCount == 0) return
    if (animated && firstVisibleItemIndex < SCROLL_ANIMATION_THRESHOLD) {
        animateScrollToItem(0)
    } else if (firstVisibleItemIndex < SCROLL_ANIMATION_THRESHOLD * 4) {
        animateScrollToItem(0)
    } else {
        scrollToItem(0)
    }
}

/** Chronological feed index → display index (newest = 0, reverseLayout). */
fun displayIndexForFeedIndex(feedIndex: Int, feedSize: Int): Int =
    (feedSize - 1 - feedIndex).coerceAtLeast(0)

/** Skip centering when the row is already within this many px of viewport center. */
private const val QUOTE_CENTER_TOLERANCE_PX = 4f

/** Slow start + slow settle, no positional overshoot. */
private val chatScrollEasing: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

private fun centeringScrollSpec(deltaPx: Float): TweenSpec<Float> {
    val durationMs = (320 + abs(deltaPx) * 0.22f).toInt().coerceIn(320, 500)
    return tween(durationMillis = durationMs, easing = chatScrollEasing)
}

/**
 * Pixel delta to move [index] so its vertical center matches the viewport center.
 * Uses live [layoutInfo] — never [scrollOffset] (positive offset breaks reverseLayout).
 */
private fun LazyListState.centeringScrollDeltaFor(index: Int): Float? {
    val item = layoutInfo.visibleItemsInfo.find { it.index == index } ?: return null
    val viewportStart = layoutInfo.viewportStartOffset
    val viewportEnd = layoutInfo.viewportEndOffset
    if (viewportEnd <= viewportStart) return null
    val viewportCenter = (viewportStart + viewportEnd) / 2f
    return item.offset + item.size / 2f - viewportCenter
}

/**
 * Center the target bubble row — same path that worked for grouped rows.
 * Each bubble is one lazy row, so row center = bubble center.
 */
suspend fun LazyListState.scrollToQuoteTarget(displayIndex: Int) {
    if (layoutInfo.totalItemsCount == 0) return
    val index = displayIndex.coerceIn(0, layoutInfo.totalItemsCount - 1)

    val alreadyVisible = layoutInfo.visibleItemsInfo.any { it.index == index }
    if (!alreadyVisible) {
        animateScrollToItem(index)
        withFrameNanos { }
    }

    var delta = centeringScrollDeltaFor(index)
    if (delta == null) {
        withFrameNanos { }
        delta = centeringScrollDeltaFor(index)
    }
    delta ?: return
    if (abs(delta) < QUOTE_CENTER_TOLERANCE_PX) return

    animateScrollBy(delta, centeringScrollSpec(delta))
}

@Composable
fun rememberShowChatScrollToBottom(listState: LazyListState) = remember {
    derivedStateOf { listState.shouldShowScrollToBottomFab() }
}

/**
 * Scroll-to-bottom — same chrome as group header bell ([LabDimens.DetailBackButtonSize], [LabDimens.HomeBarPadding]).
 */
@Composable
fun ChatScrollToBottomButton(
    visible: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(100)) + scaleIn(tween(100), initialScale = 0.85f),
        exit = fadeOut(tween(50)) + scaleOut(tween(50), targetScale = 0.85f),
    ) {
        FrostedCircleButton(
            onClick = onClick,
            hazeState = hazeState,
            modifier = Modifier
                .size(LabDimens.DetailBackButtonSize)
                .semantics { this.contentDescription = contentDescription },
        ) {
            LabIcon(LabIconName.ArrowDown, 16.dp, LabColors.White33)
        }
    }
}
