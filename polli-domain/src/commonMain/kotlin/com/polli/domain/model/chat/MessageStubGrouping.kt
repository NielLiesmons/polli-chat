package com.polli.domain.model.chat

import com.polli.core.Spacing
import com.polli.core.chat.GroupableMessage
import com.polli.core.chat.MessageGroupLayout

private class StubGroupable(
    override val id: Int,
    override val timestamp: Long,
    override val isOutgoing: Boolean,
    override val authorKey: String,
) : GroupableMessage

private fun MessageStub.asGroupable(): GroupableMessage =
    StubGroupable(
        id = id,
        timestamp = timestamp,
        isOutgoing = isOutgoing,
        authorKey = authorKey,
    )

fun layoutsForMessageStubs(stubsInChronOrder: List<MessageStub>): Map<Int, MessageGroupLayout> {
    if (stubsInChronOrder.isEmpty()) return emptyMap()
    val groupables = stubsInChronOrder.map { it.asGroupable() }
    val out = HashMap<Int, MessageGroupLayout>(stubsInChronOrder.size)
    groupables.forEachIndexed { i, msg ->
        val isFirst =
            i == 0 ||
                !continuesStubGroup(groupables[i - 1], msg) ||
                stubsInChronOrder[i].isEdited
        val isLast = i == groupables.lastIndex || !continuesStubGroup(msg, groupables[i + 1])
        out[stubsInChronOrder[i].id] = MessageGroupLayout(isFirst, isLast)
    }
    return out
}

private fun continuesStubGroup(older: GroupableMessage, newer: GroupableMessage): Boolean {
    if (older.isOutgoing != newer.isOutgoing) return false
    if (older.authorKey != newer.authorKey) return false
    val gap = (newer.timestamp - older.timestamp).coerceAtLeast(0)
    return gap <= Spacing.GROUP_MAX_GAP_SECS
}

fun List<FeedItem>.withGroupLayouts(stubFor: (Int) -> MessageStub?): List<FeedItem> {
    if (none { it is FeedItem.Message }) return this
    val chronStubs =
        mapNotNull { item ->
            (item as? FeedItem.Message)?.msgId?.let { id -> stubFor(id)?.let { id to it } }
        }
    if (chronStubs.isEmpty()) return this
    val layouts = layoutsForMessageStubs(chronStubs.map { it.second })
    return map { item ->
        when (item) {
            is FeedItem.Message ->
                item.copy(groupLayout = layouts[item.msgId] ?: item.groupLayout)
            else -> item
        }
    }
}
