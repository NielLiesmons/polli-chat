package com.polli.core.chat

import com.polli.core.Spacing

data class MessageGroupLayout(
    val isFirstInStack: Boolean = true,
    val isLastInStack: Boolean = true,
)

/** Minimal message shape for grouping (platform-agnostic). */
interface GroupableMessage {
    val id: Int
    val timestamp: Long
    val isOutgoing: Boolean
    val authorKey: String
}

fun continuesGroup(older: GroupableMessage, newer: GroupableMessage): Boolean {
    if (older.isOutgoing != newer.isOutgoing) return false
    if (older.authorKey != newer.authorKey) return false
    val gap = newer.timestamp - older.timestamp
    return gap in 0..Spacing.GROUP_MAX_GAP_SECS
}

fun layoutsForMessages(messages: List<GroupableMessage>): Map<Int, MessageGroupLayout> {
    if (messages.isEmpty()) return emptyMap()
    val out = HashMap<Int, MessageGroupLayout>(messages.size)
    messages.forEachIndexed { i, msg ->
        val isFirst = i == 0 || !continuesGroup(messages[i - 1], msg)
        val isLast = i == messages.lastIndex || !continuesGroup(msg, messages[i + 1])
        out[msg.id] = MessageGroupLayout(isFirst, isLast)
    }
    return out
}
