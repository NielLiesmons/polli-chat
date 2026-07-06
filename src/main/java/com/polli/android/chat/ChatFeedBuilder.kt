package com.polli.android.chat

import android.content.Context
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcMsg
import org.thoughtcrime.securesms.util.DateUtils

/** Builds [FeedItem] rows from a DC message id list (with optional day markers). */
object ChatFeedBuilder {
    fun build(
        context: Context,
        dc: DcContext,
        ids: IntArray,
        showNewMessages: Boolean,
    ): List<FeedItem> {
        val firstUnreadId =
            if (showNewMessages) {
                findFirstUnreadIncomingId(dc, ids)
            } else {
                null
            }
        val items = ArrayList<FeedItem>(ids.size + 2)
        for (i in ids.indices) {
            val id = ids[i]
            when {
                id == DcMsg.DC_MSG_ID_DAYMARKER -> {
                    val ts = dayMarkerTimestamp(dc, ids, i)
                    if (ts > 0) {
                        items.add(
                            FeedItem.DayMarker(
                                label = DateUtils.getRelativeDate(context, ts * 1000),
                                dayKey = ts / 86_400,
                            ),
                        )
                    }
                }
                id == firstUnreadId -> {
                    items.add(FeedItem.NewMessages)
                    items.add(FeedItem.Message(id))
                }
                id > DcMsg.DC_MSG_ID_DAYMARKER -> {
                    items.add(FeedItem.Message(id))
                }
            }
        }
        return items
    }

    private fun dayMarkerTimestamp(dc: DcContext, ids: IntArray, markerIndex: Int): Long {
        for (j in markerIndex + 1 until ids.size) {
            val id = ids[j]
            if (id <= DcMsg.DC_MSG_ID_DAYMARKER) continue
            val msg = dc.getMsg(id)
            if (msg.isOk) {
                return normalizeFeedTimestamp(msg.timestamp)
            }
        }
        for (j in markerIndex - 1 downTo 0) {
            val id = ids[j]
            if (id <= DcMsg.DC_MSG_ID_DAYMARKER) continue
            val msg = dc.getMsg(id)
            if (msg.isOk) {
                return normalizeFeedTimestamp(msg.timestamp)
            }
        }
        return 0
    }

    private fun findFirstUnreadIncomingId(dc: DcContext, ids: IntArray): Int? {
        for (id in ids) {
            if (id <= DcMsg.DC_MSG_ID_DAYMARKER) continue
            val msg = dc.getMsg(id)
            if (!msg.isOk || msg.isOutgoing || msg.isInfo) continue
            if (!msg.isSeen) return id
        }
        return null
    }

    private fun normalizeFeedTimestamp(ts: Long): Long =
        if (ts > 1_000_000_000_000L) ts / 1000 else ts
}

fun FeedItem.stableRowId(): Long =
    when (this) {
        is FeedItem.DayMarker -> -(dayKey + 1_000_000L)
        FeedItem.NewMessages -> -2L
        is FeedItem.Message -> msgId.toLong()
    }

fun List<FeedItem>.messageCount(): Int = count { it is FeedItem.Message }

fun List<FeedItem>.displayIndexForMessage(msgId: Int): Int {
    val chron = indexOfFirst { it is FeedItem.Message && it.msgId == msgId }
    return if (chron < 0) -1 else size - 1 - chron
}

fun List<FeedItem>.messageIdAtDisplayIndex(displayIndex: Int): Int? {
    val chron = size - 1 - displayIndex
    return (getOrNull(chron) as? FeedItem.Message)?.msgId
}
