package com.polli.android.stories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcMsg
import com.polli.domain.model.InboxItem
import com.polli.android.navigation.AppNav
import org.thoughtcrime.securesms.connect.DcHelper

/** Seconds in 24 hours — story rings only highlight posts within this window. */
const val STORY_RECENT_WINDOW_SEC = 86_400L

enum class StoryRingStyle {
    /** New posts in the last 24h, not yet read. Full accent ring. */
    Unread,
    /** Posts in the last 24h, all read. Dim accent ring (16% opacity). */
    ReadRecent,
    /** No posts in the last 24h. Transparent ring — opens profile (later). */
    Stale,
}

data class StoryRingEntry(
    val channel: InboxItem,
    val style: StoryRingStyle,
    /** Newest story timestamp (Unix seconds). Used for ordering. */
    val newestStoryAt: Long,
)

object ChannelStoryRingLogic {

    fun buildEntries(dc: DcContext, channels: List<InboxItem>, nowSec: Long = nowSec()): List<StoryRingEntry> {
        return channels
            .map { analyze(dc, it, nowSec) }
            .sortedWith(
                compareBy<StoryRingEntry> { it.style.sortRank }
                    .thenByDescending { it.newestStoryAt },
            )
    }

    fun storyChannelIds(entries: List<StoryRingEntry>): List<Int> =
        entries.filter { it.style != StoryRingStyle.Stale }.map { it.channel.chatId }

    /** Posts suitable for the story viewer — story content from the last 24h only. */
    fun recentStoryPosts(dc: DcContext, chatId: Int, nowSec: Long = nowSec()): List<DcMsg> {
        val cutoff = nowSec - STORY_RECENT_WINDOW_SEC
        return AppNav.loadChannelPosts(dc, chatId).filter { it.timestamp >= cutoff }
    }

    private fun analyze(dc: DcContext, channel: InboxItem, nowSec: Long): StoryRingEntry {
        val cutoff = nowSec - STORY_RECENT_WINDOW_SEC
        val posts = AppNav.loadChannelPosts(dc, channel.chatId)
        val recentPosts = posts.filter { it.timestamp >= cutoff }
        val newestAt = recentPosts.maxOfOrNull { it.timestamp }
            ?: posts.maxOfOrNull { it.timestamp }
            ?: channel.updatedAt

        val style = when {
            recentPosts.isEmpty() -> StoryRingStyle.Stale
            hasUnreadRecent(dc, channel.chatId, recentPosts) -> StoryRingStyle.Unread
            else -> StoryRingStyle.ReadRecent
        }

        return StoryRingEntry(
            channel = channel,
            style = style,
            newestStoryAt = newestAt,
        )
    }

    private fun hasUnreadRecent(dc: DcContext, chatId: Int, recentPosts: List<DcMsg>): Boolean {
        if (dc.getFreshMsgCount(chatId) > 0) return true
        return recentPosts.any { it.getState() == DcMsg.DC_STATE_IN_FRESH }
    }

    private fun nowSec(): Long = System.currentTimeMillis() / 1000

    private val StoryRingStyle.sortRank: Int
        get() = when (this) {
            StoryRingStyle.Unread -> 0
            StoryRingStyle.ReadRecent -> 1
            StoryRingStyle.Stale -> 2
        }
}

@Composable
fun rememberStoryRingEntries(
    channels: List<InboxItem>,
    refreshKey: Int = 0,
): List<StoryRingEntry> {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(emptyList<StoryRingEntry>()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun reload() {
        val dc = DcHelper.getContext(context)
        entries = ChannelStoryRingLogic.buildEntries(dc, channels)
    }

    LaunchedEffect(channels, refreshKey) {
        reload()
    }

    DisposableEffect(lifecycleOwner, channels) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return entries
}
