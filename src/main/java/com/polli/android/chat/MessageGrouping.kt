package com.polli.android.chat

import com.polli.domain.model.chat.FeedItem
import com.polli.domain.model.chat.MessageStub

/** Same author, same direction, ≤21 min gap → stack (polli parity). */
const val GROUP_MAX_GAP_SECS: Long = 21 * 60

data class MessageGroupLayout(
    val isFirstInStack: Boolean = true,
    val isLastInStack: Boolean = true,
)

/** Whether [newer] continues the same stack immediately after [older] (older is above newer). */
fun continuesGroup(older: MessageStub, newer: MessageStub): Boolean {
    if (older.isOutgoing != newer.isOutgoing) return false
    if (older.authorKey != newer.authorKey) return false
    if (newer.isEdited) return false
    val gap = (newer.timestamp - older.timestamp).coerceAtLeast(0)
    return gap <= GROUP_MAX_GAP_SECS
}

fun layoutBetweenNeighbors(
    older: MessageStub?,
    self: MessageStub,
    newer: MessageStub?,
): MessageGroupLayout {
    val isFirst = older == null ||
        !continuesGroup(older, self) ||
        self.isEdited
    val isLast = newer == null ||
        !continuesGroup(self, newer)
    return MessageGroupLayout(isFirst, isLast)
}

fun layoutsForStubs(stubs: List<MessageStub>): Map<Int, MessageGroupLayout> {
    if (stubs.isEmpty()) return emptyMap()
    val out = HashMap<Int, MessageGroupLayout>(stubs.size)
    stubs.forEachIndexed { i, stub ->
        val isFirst = i == 0 ||
            !continuesGroup(stubs[i - 1], stub) ||
            stub.isEdited
        val isLast = i == stubs.lastIndex ||
            !continuesGroup(stub, stubs[i + 1])
        out[stub.id] = MessageGroupLayout(isFirst, isLast)
    }
    return out
}

/** display index 0 = newest at bottom; range.first = newest in group. */
fun displayIndexRangeForGroup(
    displayItems: List<FeedItem>,
    anchorIndex: Int,
    stubFor: (Int) -> MessageStub?,
): IntRange {
    var first = anchorIndex
    var last = anchorIndex
    while (first > 0) {
        val olderId = (displayItems[first] as? FeedItem.Message)?.msgId ?: break
        val newerId = (displayItems[first - 1] as? FeedItem.Message)?.msgId ?: break
        val older = stubFor(olderId) ?: break
        val newer = stubFor(newerId) ?: break
        if (!continuesGroup(older, newer)) break
        first--
    }
    while (last < displayItems.lastIndex) {
        val newerId = (displayItems[last] as? FeedItem.Message)?.msgId ?: break
        val olderId = (displayItems[last + 1] as? FeedItem.Message)?.msgId ?: break
        val newer = stubFor(newerId) ?: break
        val older = stubFor(olderId) ?: break
        if (!continuesGroup(older, newer)) break
        last++
    }
    return first..last
}
