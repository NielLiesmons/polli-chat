package com.polli.ui.chat

import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.FeedItem
import com.polli.domain.model.chat.MSG_ID_DAYMARKER
import com.polli.domain.model.chat.MessageStub
import com.polli.domain.model.chat.messageIdAtDisplayIndex
import com.polli.domain.model.chat.withGroupLayouts
import com.polli.domain.repository.MessageRepository

/**
 * Message store backed by [MessageRepository]. Stubs and full [ChatMessage]
 * objects hydrate lazily per row — warm caches via [preloadMessages] like DC's LRU.
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
    /** Cached adapter view of [msgIds] without day markers — avoid reallocating on every call. */
    private var adapterIds: IntArray = intArrayOf()
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

    fun refreshMessageRow(msgId: Int) {
        invalidateMessage(msgId)
        invalidateStub(msgId)
        messages.invalidateMessage(msgId)
    }

    /** Drop cached rows after engine events (edit, reaction, delivery state). */
    fun invalidateAllCaches() {
        clearMessageCache()
        synchronized(stubLock) { stubById.clear() }
        messages.clearMessageCaches()
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
        adapterIds = intArrayOf()
        feedItemsCache = emptyList()
    }

    fun feedItems(): List<FeedItem> = feedItemsCache

    fun buildFeed(
        showNewMessages: Boolean,
        freshCount: Int,
    ): List<FeedItem> {
        feedItemsCache = finalizeFeed(ChatFeedBuilder.build(formatDayLabel, messages, msgIds, showNewMessages, freshCount))
        return feedItemsCache
    }

    fun syncFeedIds(
        chatId: Int,
        showNewMessages: Boolean,
        freshCount: Int,
    ): List<FeedItem>? {
        val listItems = messages.getMessageListItems(chatId, addDaymarker = true)
        val nextFeed =
            if (listItems != null) {
                val ids = ChatFeedBuilder.listItemsToIds(listItems)
                if (ids.contentEquals(msgIds) && feedItemsCache.isNotEmpty()) return null
                msgIds = ids
                rebuildAdapterIds()
                ChatFeedBuilder.buildFromListItems(formatDayLabel, listItems, showNewMessages, freshCount)
            } else {
                val ids = messages.getMessageIds(chatId, addDaymarker = true)
                val built = ChatFeedBuilder.build(formatDayLabel, messages, ids, showNewMessages, freshCount)
                if (ids.contentEquals(msgIds) && built == feedItemsCache) return null
                msgIds = ids
                rebuildAdapterIds()
                built
            }
        val valid = msgIds.toSet()
        synchronized(stubLock) { stubById.keys.retainAll(valid) }
        pruneCache(valid)
        feedItemsCache = finalizeFeed(nextFeed)
        return feedItemsCache
    }

    /** Optimistic append when send returns before the engine list catches up (DC reloadList timing). */
    fun appendOutgoingMessage(
        msgId: Int,
        showNewMessages: Boolean,
        freshCount: Int,
    ): List<FeedItem> {
        if (msgId in msgIds) return feedItemsCache
        msgIds = msgIds + msgId
        rebuildAdapterIds()
        feedItemsCache = finalizeFeed(ChatFeedBuilder.build(formatDayLabel, messages, msgIds, showNewMessages, freshCount))
        return feedItemsCache
    }

    fun messageIds(): IntArray = msgIds

    /** Chron-ordered message + day-marker ids for the RecyclerView. */
    fun syncAdapterIds(chatId: Int): Boolean {
        val listItems = messages.getMessageListItems(chatId, addDaymarker = true)
        val nextIds =
            if (listItems != null) {
                ChatFeedBuilder.listItemsToIds(listItems)
            } else {
                messages.getMessageIds(chatId, addDaymarker = true)
            }
        if (nextIds.contentEquals(msgIds)) return false
        msgIds = nextIds
        rebuildAdapterIds()
        val valid = msgIds.toSet()
        synchronized(stubLock) { stubById.keys.retainAll(valid) }
        pruneCache(valid)
        return true
    }

    fun adapterMessageIds(): IntArray = adapterIds

    private fun rebuildAdapterIds() {
        // Include day markers as normal feed rows (not sticky overlays).
        adapterIds = msgIds.copyOf()
    }

    fun preloadMessages(msgIdsToLoad: IntArray) {
        if (msgIdsToLoad.isEmpty()) return
        messages.preloadMessages(msgIdsToLoad)
        for (id in msgIdsToLoad) {
            if (id <= MSG_ID_DAYMARKER) continue
            messages.getStub(id)?.let { stub ->
                synchronized(stubLock) { stubById[id] = stub }
            }
            messages.getMessage(id)?.let { msg ->
                synchronized(messageCache) { messageCache[id] = msg }
            }
        }
    }

    fun preloadStubsAroundDisplayIndex(displayIndex: Int, radius: Int = 40) {
        val items = feedItemsCache
        if (items.isEmpty()) return
        val center = displayIndex.coerceIn(0, items.lastIndex)
        val from = (center - radius).coerceAtLeast(0)
        val to = (center + radius).coerceAtMost(items.lastIndex)
        val ids = IntArray(to - from + 1)
        var n = 0
        for (display in from..to) {
            val msgId = items.messageIdAtDisplayIndex(display) ?: continue
            ids[n++] = msgId
        }
        if (n == 0) return
        preloadMessages(if (n == ids.size) ids else ids.copyOf(n))
    }

    fun preloadStubs() {
        preloadMessages(msgIds.filter { it > MSG_ID_DAYMARKER }.toIntArray())
    }

    fun rebuildGroupLayouts(): List<FeedItem> {
        // Grouping is resolved at bind-time on Android (DC-style neighbor checks).
        // Keeping this as a no-op preserves the API for desktop while avoiding O(N) stub loads.
        return feedItemsCache
    }

    private fun finalizeFeed(items: List<FeedItem>): List<FeedItem> = items

    private fun pruneCache(validIds: Set<Int>) {
        synchronized(messageCache) {
            messageCache.keys.retainAll(validIds)
        }
    }

    private companion object {
        const val MAX_CACHE_SIZE = 40
    }
}
