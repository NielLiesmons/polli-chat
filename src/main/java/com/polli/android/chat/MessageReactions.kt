package com.polli.android.chat

import android.content.Context
import com.polli.android.data.engine.PolliRepositories
data class ReactionReactor(
    val contactId: Int,
    val name: String,
)

data class BubbleReaction(
    val emoji: String,
    val count: Int,
    val reactors: List<ReactionReactor>,
)

object MessageReactions {
    val DEFAULT_EMOJI = listOf(
        "👍", "❤️", "😂", "😮", "😢", "🙏", "👎", "🎉", "🔥", "💯", "😍", "🤔",
    )

    private const val CACHE_MAX = 80
    private val summaryCache = object : LinkedHashMap<Int, List<BubbleReaction>>(CACHE_MAX, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<BubbleReaction>>?): Boolean {
            return size > CACHE_MAX
        }
    }

    fun sendReaction(context: Context, msgId: Int, emoji: String?) {
        val normalized = emoji?.takeIf { it.isNotBlank() } ?: ""
        PolliRepositories.messages(context).sendReaction(msgId, normalized)
        invalidateSummary(msgId)
    }

    fun invalidateSummary(msgId: Int) {
        synchronized(summaryCache) {
            summaryCache.remove(msgId)
        }
    }

    fun cachedSummary(msgId: Int): List<BubbleReaction>? =
        synchronized(summaryCache) {
            summaryCache[msgId]
        }

    /** Warm LRU off the UI thread so bind-time load is cache-only (DC bind is sync but fast). */
    fun preloadSummaries(context: android.content.Context, msgIds: IntArray) {
        for (msgId in msgIds) {
            if (msgId <= 0) continue
            val alreadyCached =
                synchronized(summaryCache) {
                    summaryCache.containsKey(msgId)
                }
            if (alreadyCached) continue
            loadReactionSummary(context, msgId)
        }
    }

    fun loadReactionSummary(context: Context, msgId: Int): List<BubbleReaction> {
        synchronized(summaryCache) {
            summaryCache[msgId]?.let { return it }
        }
        val loaded = loadReactionSummaryUncached(context, msgId)
        synchronized(summaryCache) {
            summaryCache[msgId] = loaded
        }
        return loaded
    }

    private fun loadReactionSummaryUncached(context: Context, msgId: Int): List<BubbleReaction> {
        return PolliRepositories.messages(context).getMessageReactions(msgId).map { reaction ->
            BubbleReaction(
                emoji = reaction.emoji,
                count = reaction.count,
                reactors =
                    reaction.reactors.map { reactor ->
                        ReactionReactor(contactId = reactor.contactId, name = reactor.name)
                    },
            )
        }
    }
}
