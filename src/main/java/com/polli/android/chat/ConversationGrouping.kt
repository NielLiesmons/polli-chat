package com.polli.android.chat

import com.polli.domain.model.chat.MSG_ID_DAYMARKER
import com.polli.domain.model.chat.MessageStub
import com.polli.core.chat.MessageGroupLayout

/** Group layout from raw chron-ordered message ids (DC adapter has no FeedItem rows). */
fun groupLayoutAtChronIndex(
    chronIndex: Int,
    msgIds: IntArray,
    getStub: (Int) -> MessageStub?,
): MessageGroupLayout {
    if (chronIndex !in msgIds.indices) return MessageGroupLayout()
    val msgId = msgIds[chronIndex]
    if (msgId <= MSG_ID_DAYMARKER) return MessageGroupLayout()
    val self = getStub(msgId) ?: return MessageGroupLayout()
    var older: MessageStub? = null
    for (j in chronIndex - 1 downTo 0) {
        val id = msgIds[j]
        if (id > MSG_ID_DAYMARKER) {
            older = getStub(id)
            break
        }
    }
    var newer: MessageStub? = null
    for (j in chronIndex + 1 until msgIds.size) {
        val id = msgIds[j]
        if (id > MSG_ID_DAYMARKER) {
            newer = getStub(id)
            break
        }
    }
    return layoutBetweenNeighbors(older = older, self = self, newer = newer)
}

fun positionForMsgId(msgIds: IntArray, msgId: Int): Int {
    for (i in msgIds.indices) {
        if (msgIds[i] == msgId) return msgIds.size - 1 - i
    }
    return -1
}

fun chronIndexForPosition(position: Int, msgIds: IntArray): Int = msgIds.size - 1 - position

fun dayHeaderId(timestampSec: Long): Long {
    if (timestampSec <= 0) return -1L
    val daySec = 86_400L
    return timestampSec / daySec
}
