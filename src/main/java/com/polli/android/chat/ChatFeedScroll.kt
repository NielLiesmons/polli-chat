package com.polli.android.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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

/** Mirrors [org.thoughtcrime.securesms.ConversationFragment.scrollMaybeSmoothToMsgId]. */
private const val SMOOTH_SCROLL_DISTANCE_THRESHOLD = 15

/** Bias quoted rows upward in the viewport — single scroll step, negative offset only. */
private fun LazyListState.quoteScrollOffsetPx(): Int {
    val viewport = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    if (viewport <= 0) return 0
    return -(viewport / 3)
}

/**
 * Scroll to a feed row by display index (newest = 0).
 * Smooth when the target is within [SMOOTH_SCROLL_DISTANCE_THRESHOLD] visible rows; instant otherwise.
 */
suspend fun LazyListState.scrollMaybeSmoothToDisplayIndex(displayIndex: Int) {
    if (layoutInfo.totalItemsCount == 0) return
    val index = displayIndex.coerceIn(0, layoutInfo.totalItemsCount - 1)
    val visible = layoutInfo.visibleItemsInfo
    val first = visible.firstOrNull()?.index ?: firstVisibleItemIndex
    val last = visible.lastOrNull()?.index ?: first
    val distance = minOf(abs(index - first), abs(index - last))
    val scrollOffset = quoteScrollOffsetPx()
    if (distance < SMOOTH_SCROLL_DISTANCE_THRESHOLD) {
        animateScrollToItem(index, scrollOffset)
    } else {
        scrollToItem(index, scrollOffset)
    }
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
