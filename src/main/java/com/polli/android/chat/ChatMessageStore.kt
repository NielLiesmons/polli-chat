package com.polli.android.chat

import android.content.Context
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcMsg

/** Lightweight row metadata for grouping — built on reload, not per-compose. */
data class MessageStub(
    val id: Int,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val authorId: Int,
    val authorName: String,
    val authorColorSeed: String,
    val isEdited: Boolean,
    val isInfo: Boolean,
    val hasText: Boolean,
    val hasAttachment: Boolean,
) {
    val authorKey: String
        get() = authorColorSeed.ifBlank {
            if (authorId != 0) authorId.toString() else authorName
        }

    val isDisplayable: Boolean
        get() = !isInfo && (hasText || hasAttachment)
}

/**
 * Delta Chat–style message store: [DcContext.getChatMsgs] drives the feed; stubs and full
 * [ChatMessage] objects hydrate lazily per row (see [ConversationAdapter.getMsg]).
 */
class ChatMessageStore {
    private val messageCache = object : LinkedHashMap<Int, ChatMessage>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, ChatMessage>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }

    private val stubById = LinkedHashMap<Int, MessageStub>()
    private var msgIds: IntArray = intArrayOf()
    private var feedItemsCache: List<FeedItem> = emptyList()

    fun getMessage(dc: DcContext, msgId: Int): ChatMessage? {
        synchronized(messageCache) {
            messageCache[msgId]?.let { return it }
        }
        val msg = dc.getMsg(msgId)
        if (!msg.isOk) return null
        val chatMsg = MessageLoader.fromDcMsg(dc, msg)
        synchronized(messageCache) {
            messageCache[msgId] = chatMsg
        }
        return chatMsg
    }

    /** Grouping metadata — one [DcContext.getMsg] per id, cached (DC row bind). */
    fun getStub(dc: DcContext, msgId: Int): MessageStub? {
        stubById[msgId]?.let { return it }
        if (msgId <= DcMsg.DC_MSG_ID_DAYMARKER) return null
        val msg = dc.getMsg(msgId)
        if (!msg.isOk) return null
        val stub = MessageLoader.stubFromDcMsg(dc, msg)
        if (!stub.isDisplayable) return null
        stubById[msgId] = stub
        return stub
    }

    fun invalidateMessage(msgId: Int) {
        synchronized(messageCache) {
            messageCache.remove(msgId)
        }
    }

    fun invalidateStub(msgId: Int) {
        stubById.remove(msgId)
    }

    fun clearMessageCache() {
        synchronized(messageCache) {
            messageCache.clear()
        }
    }

    fun reset() {
        clearMessageCache()
        stubById.clear()
        msgIds = intArrayOf()
        feedItemsCache = emptyList()
    }

    fun feedItems(): List<FeedItem> = feedItemsCache

    fun buildFeed(context: Context, dc: DcContext, showNewMessages: Boolean): List<FeedItem> {
        feedItemsCache = ChatFeedBuilder.build(context, dc, msgIds, showNewMessages)
        return feedItemsCache
    }

    /**
     * Instant feed from [DcContext.getChatMsgs] only. Returns null when the id list is unchanged
     * and the rendered feed matches, so callers can skip a no-op [feedItems] assignment.
     */
    fun syncFeedIds(
        context: Context,
        dc: DcContext,
        chatId: Int,
        showNewMessages: Boolean,
    ): List<FeedItem>? {
        val ids = dc.getChatMsgs(chatId, DcContext.DC_GCM_ADDDAYMARKER, 0) ?: intArrayOf()
        val nextFeed = ChatFeedBuilder.build(context, dc, ids, showNewMessages)
        if (ids.contentEquals(msgIds) && nextFeed == feedItemsCache) return null
        msgIds = ids
        val valid = ids.toSet()
        stubById.keys.retainAll(valid)
        pruneCache(valid)
        feedItemsCache = nextFeed
        return feedItemsCache
    }

    fun messageIds(): IntArray = msgIds

    /** Warm stub cache off the main thread — does not touch the feed list. */
    fun preloadStubs(dc: DcContext) {
        for (id in msgIds) {
            if (id <= DcMsg.DC_MSG_ID_DAYMARKER) continue
            getStub(dc, id)
        }
    }

    /** Warm stubs for the first paint window around the open scroll position (DC binds ~15 rows). */
    fun preloadStubsAroundDisplayIndex(dc: DcContext, displayIndex: Int, radius: Int = 12) {
        val items = feedItemsCache
        if (items.isEmpty()) return
        val center = displayIndex.coerceIn(0, items.lastIndex)
        val from = (center - radius).coerceAtLeast(0)
        val to = (center + radius).coerceAtMost(items.lastIndex)
        for (display in from..to) {
            val msgId = items.messageIdAtDisplayIndex(display) ?: continue
            getStub(dc, msgId)
        }
    }

    /**
     * Fast path for [DcContext.DC_EVENT_INCOMING_MSG]: append when ids grew by one at the tail.
     * Falls back to null so the caller can run [syncFeedIds].
     */
    fun tryAppendIncoming(
        context: Context,
        dc: DcContext,
        chatId: Int,
        msgId: Int,
        showNewMessages: Boolean,
    ): List<FeedItem>? {
        val newIds = dc.getChatMsgs(chatId, DcContext.DC_GCM_ADDDAYMARKER, 0) ?: return null
        if (newIds.size != msgIds.size + 1) return null
        for (i in msgIds.indices) {
            if (msgIds[i] != newIds[i]) return null
        }
        if (newIds.last() != msgId) return null
        msgIds = newIds
        getStub(dc, msgId)
        feedItemsCache = ChatFeedBuilder.build(context, dc, newIds, showNewMessages)
        return feedItemsCache
    }

    private fun pruneCache(validIds: Set<Int>) {
        synchronized(messageCache) {
            messageCache.keys.retainAll(validIds)
        }
    }

    private companion object {
        const val MAX_CACHE_SIZE = 40
    }
}
