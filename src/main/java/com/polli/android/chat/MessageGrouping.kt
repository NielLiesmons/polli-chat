package com.polli.android.chat

/** Same author, same direction, ≤21 min gap → stack (polli parity). */
const val GROUP_MAX_GAP_SECS: Long = 21 * 60

data class MessageGroupLayout(
    val isFirstInStack: Boolean = true,
    val isLastInStack: Boolean = true,
)

/** Whether [newer] continues the same stack immediately after [older] (older is above newer). */
fun continuesGroup(older: ChatMessage, newer: ChatMessage): Boolean {
    if (older.isOutgoing != newer.isOutgoing) return false
    if (older.authorKey != newer.authorKey) return false
    if (newer.isEdited) return false
    val gap = (newer.timestamp - older.timestamp).coerceAtLeast(0)
    return gap <= GROUP_MAX_GAP_SECS
}

fun layoutsForMessages(messages: List<ChatMessage>): Map<Int, MessageGroupLayout> {
    if (messages.isEmpty()) return emptyMap()
    val out = HashMap<Int, MessageGroupLayout>(messages.size)
    messages.forEachIndexed { i, msg ->
        val isFirst = i == 0 ||
            !continuesGroup(messages[i - 1], msg) ||
            msg.isEdited
        val isLast = i == messages.lastIndex ||
            !continuesGroup(msg, messages[i + 1])
        out[msg.id] = MessageGroupLayout(isFirst, isLast)
    }
    return out
}
