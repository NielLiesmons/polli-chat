package com.polli.ui.chat

import com.polli.domain.model.chat.FeedItem
import com.polli.domain.model.chat.MSG_ID_DAYMARKER
import com.polli.domain.repository.MessageRepository

/** Builds [FeedItem] rows from engine message ids (with optional day markers). */
object ChatFeedBuilder {
    fun build(
        formatDayLabel: (timestampSec: Long) -> String,
        messages: MessageRepository,
        ids: IntArray,
        showNewMessages: Boolean,
        freshCount: Int = 0,
    ): List<FeedItem> {
        val firstUnreadId =
            if (showNewMessages && freshCount > 0) {
                findFirstUnreadIncomingId(ids, freshCount)
            } else {
                null
            }
        val items = ArrayList<FeedItem>(ids.size + 2)
        for (i in ids.indices) {
            val id = ids[i]
            when {
                id == MSG_ID_DAYMARKER -> {
                    val ts = dayMarkerTimestamp(messages, ids, i)
                    if (ts > 0) {
                        items.add(
                            FeedItem.DayMarker(
                                label = formatDayLabel(ts),
                                dayKey = ts / 86_400,
                            ),
                        )
                    }
                }
                id == firstUnreadId -> {
                    items.add(FeedItem.NewMessages)
                    items.add(FeedItem.Message(id))
                }
                id > MSG_ID_DAYMARKER -> {
                    items.add(FeedItem.Message(id))
                }
            }
        }
        return items
    }

    private fun dayMarkerTimestamp(messages: MessageRepository, ids: IntArray, markerIndex: Int): Long {
        for (j in markerIndex + 1 until ids.size) {
            val id = ids[j]
            if (id <= MSG_ID_DAYMARKER) continue
            val ts = messages.getStub(id)?.timestamp ?: messages.getMessage(id)?.timestamp
            if (ts != null && ts > 0) return ts
        }
        for (j in markerIndex - 1 downTo 0) {
            val id = ids[j]
            if (id <= MSG_ID_DAYMARKER) continue
            val ts = messages.getStub(id)?.timestamp ?: messages.getMessage(id)?.timestamp
            if (ts != null && ts > 0) return ts
        }
        return 0
    }

    private fun findFirstUnreadIncomingId(ids: IntArray, freshCount: Int): Int? {
        var seen = 0
        for (i in ids.indices.reversed()) {
            val id = ids[i]
            if (id <= MSG_ID_DAYMARKER) continue
            seen++
            if (seen == freshCount) return id
        }
        return null
    }
}
