package com.polli.ui.chat

import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.FeedItem
import com.polli.domain.model.chat.MSG_ID_DAYMARKER
import com.polli.domain.model.chat.MessageStub
import com.polli.domain.model.chat.messageIdAtDisplayIndex
import com.polli.domain.repository.MessageRepository

/**
 * Message store backed by [MessageRepository] (JSON-RPC). Stubs and full [ChatMessage]
 * objects hydrate lazily per row.
 */
class ChatMessageStore(
    private val messages: MessageRepository,
    private val formatDayLabel: (timestampSec: Long) -> String,
) {
    private val messageCache = object : LinkedHashMap<Int, ChatMessage>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, ChatMessage>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }

    private val stubById = LinkedHashMap<Int, MessageStub>()
    private val stubLock = Any()
    private var msgIds: IntArray = intArrayOf()
    private var feedItemsCache: List<FeedItem> = emptyList()

    fun getMessage(msgId: Int): ChatMessage? {
        synchronized(messageCache) {
            messageCache[msgId]?.let { return it }
        }
        val chatMsg = messages.getMessage(msgId) ?: return null
        synchronized(messageCache) {
            messageCache[msgId] = chatMsg
        }
        return chatMsg
    }

    fun getStub(msgId: Int): MessageStub? {
        synchronized(stubLock) { stubById[msgId]?.let { return it } }
        if (msgId <= MSG_ID_DAYMARKER) return null
        val stub = messages.getStub(msgId) ?: return null
        if (!stub.isDisplayable) return null
        synchronized(stubLock) { stubById[msgId] = stub }
        return stub
    }

    fun invalidateMessage(msgId: Int) {
        synchronized(messageCache) {
            messageCache.remove(msgId)
        }
    }

    fun invalidateStub(msgId: Int) {
        synchronized(stubLock) { stubById.remove(msgId) }
    }

    /** Drop cached rows after engine events (edit, reaction, delivery state). */
    fun invalidateAllCaches() {
        clearMessageCache()
        synchronized(stubLock) { stubById.clear() }
    }

    fun clearMessageCache() {
        synchronized(messageCache) {
            messageCache.clear()
        }
    }

    fun reset() {
        clearMessageCache()
        synchronized(stubLock) { stubById.clear() }
        msgIds = intArrayOf()
        feedItemsCache = emptyList()
    }

    fun feedItems(): List<FeedItem> = feedItemsCache

    fun buildFeed(
        showNewMessages: Boolean,
        freshCount: Int,
    ): List<FeedItem> {
        feedItemsCache = ChatFeedBuilder.build(formatDayLabel, messages, msgIds, showNewMessages, freshCount)
        return feedItemsCache
    }

    fun syncFeedIds(
        chatId: Int,
        showNewMessages: Boolean,
        freshCount: Int,
    ): List<FeedItem>? {
        val ids = messages.getMessageIds(chatId, addDaymarker = true)
        val nextFeed = ChatFeedBuilder.build(formatDayLabel, messages, ids, showNewMessages, freshCount)
        if (ids.contentEquals(msgIds) && nextFeed == feedItemsCache) return null
        msgIds = ids
        val valid = ids.toSet()
        synchronized(stubLock) { stubById.keys.retainAll(valid) }
        pruneCache(valid)
        feedItemsCache = nextFeed
        return feedItemsCache
    }

    fun messageIds(): IntArray = msgIds

    fun preloadStubs() {
        for (id in msgIds) {
            if (id <= MSG_ID_DAYMARKER) continue
            getStub(id)
        }
    }

    fun preloadStubsAroundDisplayIndex(displayIndex: Int, radius: Int = 12) {
        val items = feedItemsCache
        if (items.isEmpty()) return
        val center = displayIndex.coerceIn(0, items.lastIndex)
        val from = (center - radius).coerceAtLeast(0)
        val to = (center + radius).coerceAtMost(items.lastIndex)
        for (display in from..to) {
            val msgId = items.messageIdAtDisplayIndex(display) ?: continue
            getStub(msgId)
        }
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
