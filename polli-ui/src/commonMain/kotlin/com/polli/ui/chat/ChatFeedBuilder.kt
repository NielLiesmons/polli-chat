package com.polli.ui.chat

import com.polli.domain.model.chat.ChatListItem
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
        return attachMessageNeighbors(items)
    }

    /** Preferred path — one RPC, day markers include timestamps (no per-marker message loads). */
    fun buildFromListItems(
        formatDayLabel: (timestampSec: Long) -> String,
        listItems: List<ChatListItem>,
        showNewMessages: Boolean,
        freshCount: Int = 0,
    ): List<FeedItem> {
        val messageIds =
            listItems.mapNotNull { item ->
                when (item) {
                    is ChatListItem.Message -> item.msgId
                    is ChatListItem.DayMarker -> null
                }
            }
        val firstUnreadId =
            if (showNewMessages && freshCount > 0) {
                findFirstUnreadIncomingId(messageIds.toIntArray(), freshCount)
            } else {
                null
            }
        val items = ArrayList<FeedItem>(listItems.size + 1)
        for (item in listItems) {
            when (item) {
                is ChatListItem.DayMarker -> {
                    val tsSec = item.timestampMs / 1000
                    if (tsSec > 0) {
                        items.add(
                            FeedItem.DayMarker(
                                label = formatDayLabel(tsSec),
                                dayKey = tsSec / 86_400,
                            ),
                        )
                    }
                }
                is ChatListItem.Message -> {
                    if (item.msgId == firstUnreadId) {
                        items.add(FeedItem.NewMessages)
                    }
                    items.add(FeedItem.Message(item.msgId))
                }
            }
        }
        return attachMessageNeighbors(items)
    }

    fun listItemsToIds(listItems: List<ChatListItem>): IntArray {
        val ids = ArrayList<Int>(listItems.size)
        for (item in listItems) {
            when (item) {
                is ChatListItem.DayMarker -> ids.add(MSG_ID_DAYMARKER)
                is ChatListItem.Message -> ids.add(item.msgId)
            }
        }
        return ids.toIntArray()
    }

    /** Precompute grouping neighbors once per feed build (DC binds with position context). */
    fun attachMessageNeighbors(items: List<FeedItem>): List<FeedItem> {
        if (items.none { it is FeedItem.Message }) return items
        val out = ArrayList<FeedItem>(items.size)
        var lastMsgId: Int? = null
        for (i in items.indices) {
            when (val row = items[i]) {
                is FeedItem.Message -> {
                    var newer: Int? = null
                    for (j in i + 1 until items.size) {
                        val next = items[j]
                        if (next is FeedItem.Message) {
                            newer = next.msgId
                            break
                        }
                    }
                    out.add(row.copy(olderMsgId = lastMsgId, newerMsgId = newer))
                    lastMsgId = row.msgId
                }
                else -> out.add(row)
            }
        }
        return out
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
