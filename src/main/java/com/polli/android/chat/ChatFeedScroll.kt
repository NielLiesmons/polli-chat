package com.polli.android.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

private val chatScrollSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/** Same motion feel but settles quickly so overlay chrome can appear right after centering. */
private val chatScrollOverlaySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
)

/**
 * Pixel delta to move [index] so its vertical center matches the viewport center.
 * Uses live layout coordinates so we never guess [scrollOffset] sign for reverseLayout.
 */
private fun LazyListState.centeringScrollDeltaFor(index: Int): Float? {
    val item = layoutInfo.visibleItemsInfo.find { it.index == index } ?: return null
    val viewportStart = layoutInfo.viewportStartOffset
    val viewportEnd = layoutInfo.viewportEndOffset
    if (viewportEnd <= viewportStart) return null
    val viewportCenter = (viewportStart + viewportEnd) / 2f
    val itemCenter = item.offset + item.size / 2f
    return itemCenter - viewportCenter
}

/**
 * Center the target row in the viewport — smooth spring scroll (quote tap, actions, search).
 */
suspend fun LazyListState.scrollToQuoteTarget(
    displayIndex: Int,
    forOverlay: Boolean = false,
) {
    if (layoutInfo.totalItemsCount == 0) return
    val index = displayIndex.coerceIn(0, layoutInfo.totalItemsCount - 1)
    val settleSpring: AnimationSpec<Float> = if (forOverlay) chatScrollOverlaySpring else chatScrollSpring

    val alreadyVisible = layoutInfo.visibleItemsInfo.any { it.index == index }
    if (!alreadyVisible) {
        animateScrollToItem(index)
    }

    val delta = centeringScrollDeltaFor(index) ?: return
    if (kotlin.math.abs(delta) < QUOTE_CENTER_TOLERANCE_PX) return

    animateScrollBy(delta, settleSpring)
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
