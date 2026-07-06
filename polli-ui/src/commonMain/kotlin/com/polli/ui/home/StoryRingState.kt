package com.polli.ui.home

import com.polli.domain.model.InboxItem

const val STORY_RECENT_WINDOW_SEC = 86_400L

enum class StoryRingStyle {
    Unread,
    ReadRecent,
    Stale,
}

data class StoryRingEntry(
    val channel: InboxItem,
    val style: StoryRingStyle,
    val newestStoryAt: Long,
)

data class StoryLaunchBounds(
    val centerX: Float,
    val centerY: Float,
    val size: Float,
)

data class StorySession(
    val channelId: Int,
    val channelIds: List<Int>,
    val launchBounds: StoryLaunchBounds,
)

object StoryRingLogic {
    fun buildEntries(
        channels: List<InboxItem>,
        nowSec: Long = System.currentTimeMillis() / 1000,
    ): List<StoryRingEntry> {
        return channels
            .map { analyze(it, nowSec) }
            .sortedWith(
                compareBy<StoryRingEntry> { it.style.sortRank }
                    .thenByDescending { it.newestStoryAt },
            )
    }

    fun storyChannelIds(entries: List<StoryRingEntry>): List<Int> =
        entries.filter { it.style != StoryRingStyle.Stale }.map { it.channel.chatId }

    private fun analyze(channel: InboxItem, nowSec: Long): StoryRingEntry {
        val cutoff = nowSec - STORY_RECENT_WINDOW_SEC
        val recent = channel.updatedAt >= cutoff
        val style =
            when {
                !recent -> StoryRingStyle.Stale
                channel.unreadCount > 0 -> StoryRingStyle.Unread
                else -> StoryRingStyle.ReadRecent
            }
        return StoryRingEntry(
            channel = channel,
            style = style,
            newestStoryAt = channel.updatedAt,
        )
    }

    private val StoryRingStyle.sortRank: Int
        get() =
            when (this) {
                StoryRingStyle.Unread -> 0
                StoryRingStyle.ReadRecent -> 1
                StoryRingStyle.Stale -> 2
            }
}
