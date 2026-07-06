package com.polli.android.chat

import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.messageCount
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.polli.android.settings.LocalAppPrefs
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.thoughtcrime.securesms.components.audioplay.AudioPlaybackViewModel

/**
 * DC [org.thoughtcrime.securesms.ConversationFragment] list host — [RecyclerView] with reverse
 * [LinearLayoutManager], stable ids, and per-row bind. Polli bubbles stay Compose inside recycled views.
 */
@Composable
fun ChatFeedRecycler(
    viewModel: ChatViewModel,
    reloadGeneration: Int,
    headerClearance: Dp,
    feedBottomPadding: Dp,
    hazeState: HazeState,
    scrollController: ChatRecyclerController,
    playbackViewModel: AudioPlaybackViewModel?,
    uiScaleRevision: Int,
    onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
    onScrollToMessage: (Int) -> Unit,
    onFirstLoadScrollDone: () -> Unit,
    onScrolledToBottom: () -> Unit,
    onUnreadBelow: (Int) -> Unit,
    consumeScrollToBottomOnReload: () -> Boolean,
) {
    val prefs = LocalAppPrefs.current
    val density = LocalDensity.current
    val topPadPx = remember(headerClearance, density) { with(density) { headerClearance.roundToPx() } }
    val bottomPadPx = remember(feedBottomPadding, density) { with(density) { feedBottomPadding.roundToPx() } }

    val adapter =
        remember(prefs, uiScaleRevision) {
            PolliChatFeedAdapter(
                viewModel = viewModel,
                prefs = prefs,
                uiScaleRevision = uiScaleRevision,
                playbackViewModel = playbackViewModel,
                onOpenMessageOverlay = onOpenMessageOverlay,
                onQuoteClick = onScrollToMessage,
            ).also { it.changeData(viewModel.feedItems) }
        }

    var prevMsgCount by remember { mutableIntStateOf(0) }
    var appliedGeneration by remember { mutableIntStateOf(-1) }

    fun applyStartingPosition(layoutManager: PolliChatLayoutManager, msgCount: Int) {
        if (!viewModel.pendingFirstLoadScroll || msgCount <= 0) return
        val target = viewModel.initialScrollIndex.coerceIn(0, msgCount - 1)
        layoutManager.setStartingPosition(target)
    }

    fun afterFeedSync(recycler: RecyclerView, msgCount: Int) {
        val msgDelta = msgCount - prevMsgCount
        recycler.post {
            when {
                viewModel.pendingFirstLoadScroll && msgCount > 0 -> {
                    scrollController.updateScrollFabVisibility()
                    onFirstLoadScrollDone()
                    onScrolledToBottom()
                }
                consumeScrollToBottomOnReload() -> {
                    scrollController.scrollToBottom(
                        animated =
                            (recycler.layoutManager as? LinearLayoutManager)
                                ?.findFirstVisibleItemPosition()
                                ?.let { it < 50 } == true,
                    )
                    onScrolledToBottom()
                }
                scrollController.isAtChatBottom() -> {
                    if (msgDelta > 0) {
                        scrollController.scrollToBottom(
                            animated =
                                (recycler.layoutManager as? LinearLayoutManager)
                                    ?.findFirstVisibleItemPosition()
                                    ?.let { it < 50 } == true,
                        )
                    }
                    onScrolledToBottom()
                }
                msgDelta > 0 -> onUnreadBelow(msgDelta)
            }
            prevMsgCount = msgCount
        }
    }

    fun syncFeed(recycler: RecyclerView) {
        val items = viewModel.feedItems
        val layoutManager = recycler.layoutManager as? PolliChatLayoutManager ?: return
        if (viewModel.pendingFirstLoadScroll) {
            applyStartingPosition(layoutManager, items.size)
        }
        val preserveScroll = !viewModel.pendingFirstLoadScroll
        val anchor =
            if (preserveScroll) {
                ChatFeedScrollAnchor.capture(recycler, layoutManager, adapter.itemCount)
            } else {
                null
            }
        val changed = adapter.changeData(items)
        if (changed && anchor != null) {
            ChatFeedScrollAnchor.restore(layoutManager, anchor, adapter.itemCount)
        }
        appliedGeneration = reloadGeneration
        afterFeedSync(recycler, items.messageCount())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier =
                Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
            factory = { context ->
                val items = viewModel.feedItems
                val layoutManager =
                    PolliChatLayoutManager(context).also {
                        applyStartingPosition(it, items.size)
                    }
                adapter.changeData(items)
                RecyclerView(context).apply {
                    this.layoutManager = layoutManager
                    itemAnimator = null
                    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                    clipToPadding = false
                    setHasFixedSize(true)
                    setItemViewCacheSize(24)
                    setPadding(0, topPadPx, 0, bottomPadPx)
                    this.adapter = adapter
                    addOnScrollListener(
                        object : RecyclerView.OnScrollListener() {
                            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                scrollController.updateScrollFabVisibility()
                                if (scrollController.isAtChatBottom()) {
                                    onScrolledToBottom()
                                }
                            }
                        },
                    )
                    scrollController.recyclerView = this
                    appliedGeneration = reloadGeneration
                    afterFeedSync(this, items.messageCount())
                }
            },
            update = { recycler ->
                recycler.setPadding(0, topPadPx, 0, bottomPadPx)
                scrollController.recyclerView = recycler
                if (appliedGeneration != reloadGeneration) {
                    syncFeed(recycler)
                }
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            scrollController.recyclerView = null
        }
    }
}
