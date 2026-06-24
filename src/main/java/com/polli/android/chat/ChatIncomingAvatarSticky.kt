package com.polli.android.chat

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.polli.android.theme.LabDimens

/** Per-row geometry reported from the chat feed (root / window coordinates). */
data class IncomingRowGeometry(
    val messageId: Int,
    val authorKey: String,
    val isFirstInStack: Boolean,
    val isLastInStack: Boolean,
    val rowBounds: Rect,
    val avatarAnchor: Rect?,
)

data class StickyAvatarOverlay(
    val messageId: Int,
    val authorName: String,
    val authorKey: String,
    val authorId: Int,
    /** Top-left of avatar in root coordinates. */
    val rootTopLeftX: Float,
    val rootTopLeftY: Float,
)

/**
 * Telegram-style sticky incoming avatars.
 *
 * Stick **until the top of the first message in the group** scrolls past the sticky slot
 * (not until the avatar row leaves the screen). Uses a feed overlay so the avatar survives
 * when the bottom row is recycled by [LazyColumn].
 */
@Stable
class IncomingAvatarStickyState {
    private val rows = mutableStateMapOf<Int, IncomingRowGeometry>()
    private var stickyBottomY by mutableFloatStateOf(Float.MAX_VALUE)
    private var feedRootLeft by mutableFloatStateOf(0f)

    fun reportRow(geometry: IncomingRowGeometry?) {
        if (geometry == null) {
            return
        }
        rows[geometry.messageId] = geometry
    }

    fun clearRow(messageId: Int) {
        rows.remove(messageId)
    }

    fun updateFeedAnchor(rootLeft: Float, stickyBottom: Float) {
        feedRootLeft = rootLeft
        stickyBottomY = stickyBottom
    }

    fun isPinned(messageId: Int, listState: LazyListState, displayItems: List<FeedItem>): Boolean =
        computeOverlay(listState, displayItems)?.messageId == messageId

    @Composable
    fun overlay(
        listState: LazyListState,
        displayItems: List<FeedItem>,
    ): StickyAvatarOverlay? {
        val density = LocalDensity.current
        listState.layoutInfo.visibleItemsInfo.size
        listState.firstVisibleItemIndex
        listState.firstVisibleItemScrollOffset
        return computeOverlay(listState, displayItems, density)
    }

    private fun computeOverlay(
        listState: LazyListState,
        displayItems: List<FeedItem>,
        density: Density = Density(1f, 1f),
    ): StickyAvatarOverlay? {
        if (stickyBottomY >= Float.MAX_VALUE / 2f || rows.isEmpty()) return null

        val avatarSizePx = with(density) { LabDimens.ChatAvatarSize.toPx() }
        val stickyAvatarTopY = stickyBottomY - avatarSizePx
        val pushGapPx = with(density) { 2.dp.toPx() }

        val groups = buildVisibleIncomingGroups(displayItems, rows)
        if (groups.isEmpty()) return null

        // Bottom-most group in the feed that is still in the sticky zone.
        val candidates = groups
            .filter { group ->
                group.groupTopY < stickyAvatarTopY + pushGapPx &&
                    group.shouldPin(stickyBottomY, stickyAvatarTopY)
            }
            .sortedBy { it.newestDisplayIndex }

        val primary = candidates.firstOrNull() ?: return null
        var overlayTopY = stickyAvatarTopY

        // Older groups above push the pinned avatar upward.
        val olderGroups = groups
            .filter { it.newestDisplayIndex > primary.newestDisplayIndex }
            .sortedByDescending { it.newestDisplayIndex }

        for (group in olderGroups) {
            val upperBottom = group.avatarAnchorBottom ?: continue
            val minTop = upperBottom + pushGapPx
            if (overlayTopY < minTop) {
                overlayTopY = minTop
            }
        }

        val anchor = primary.avatarAnchor
        val rootX = anchor?.left ?: (feedRootLeft + with(density) { LabDimens.ChatRowPaddingH.toPx() })
        return StickyAvatarOverlay(
            messageId = primary.avatarMessageId,
            authorName = primary.authorName,
            authorKey = primary.authorKey,
            authorId = primary.authorId,
            rootTopLeftX = rootX,
            rootTopLeftY = overlayTopY,
        )
    }
}

private data class VisibleIncomingGroup(
    val newestDisplayIndex: Int,
    val avatarMessageId: Int,
    val authorKey: String,
    val authorName: String,
    val authorId: Int,
    val groupTopY: Float,
    val avatarAnchor: Rect?,
    val avatarAnchorBottom: Float?,
) {
    fun shouldPin(stickyBottomY: Float, stickyAvatarTopY: Float): Boolean {
        val naturalBottom = avatarAnchorBottom ?: return true
        return naturalBottom >= stickyAvatarTopY - 1f || naturalBottom > stickyBottomY - 1f
    }
}

private fun buildVisibleIncomingGroups(
    displayItems: List<FeedItem>,
    rows: Map<Int, IncomingRowGeometry>,
): List<VisibleIncomingGroup> {
    if (displayItems.isEmpty() || rows.isEmpty()) return emptyList()

    val messageIndices = HashMap<Int, Int>()
    displayItems.forEachIndexed { index, item ->
        if (item is FeedItem.Message) {
            messageIndices[item.message.id] = index
        }
    }

    val groupsByNewestIndex = LinkedHashMap<Int, VisibleIncomingGroup>()

    for ((messageId, _) in rows) {
        val startIndex = messageIndices[messageId] ?: continue
        val range = displayIndexRangeForGroup(displayItems, startIndex)
        val newestIndex = range.first
        if (groupsByNewestIndex.containsKey(newestIndex)) continue

        val newest = (displayItems[newestIndex] as? FeedItem.Message)?.message ?: continue
        if (newest.isOutgoing) continue

        val tops = range.mapNotNull { idx ->
            val id = (displayItems[idx] as FeedItem.Message).message.id
            rows[id]?.rowBounds?.top
        }
        if (tops.isEmpty()) continue

        val bottoms = range.mapNotNull { idx ->
            val id = (displayItems[idx] as FeedItem.Message).message.id
            rows[id]?.rowBounds?.bottom
        }
        val anchor = rows[newest.id]?.avatarAnchor
        val anchorBottom = anchor?.bottom ?: bottoms.maxOrNull()

        groupsByNewestIndex[newestIndex] = VisibleIncomingGroup(
            newestDisplayIndex = newestIndex,
            avatarMessageId = newest.id,
            authorKey = newest.authorKey,
            authorName = newest.authorName,
            authorId = newest.authorId,
            groupTopY = tops.min(),
            avatarAnchor = anchor,
            avatarAnchorBottom = anchorBottom,
        )
    }
    return groupsByNewestIndex.values.toList()
}

/** display index 0 = newest at bottom; range.first = newest in group. */
private fun displayIndexRangeForGroup(displayItems: List<FeedItem>, anchorIndex: Int): IntRange {
    var first = anchorIndex
    var last = anchorIndex
    while (first > 0) {
        val older = (displayItems[first] as? FeedItem.Message)?.message ?: break
        val newer = (displayItems[first - 1] as? FeedItem.Message)?.message ?: break
        if (!continuesGroup(older, newer)) break
        first--
    }
    while (last < displayItems.lastIndex) {
        val newer = (displayItems[last] as? FeedItem.Message)?.message ?: break
        val older = (displayItems[last + 1] as? FeedItem.Message)?.message ?: break
        if (!continuesGroup(older, newer)) break
        last++
    }
    return first..last
}
