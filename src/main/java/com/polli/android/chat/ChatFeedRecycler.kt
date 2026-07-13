package com.polli.android.chat

import com.polli.domain.model.chat.ChatMessage
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.polli.android.BuildConfig
import com.polli.android.settings.LocalAppPrefs
import com.polli.android.platform.PolliAudioPlaybackViewModel
import com.polli.android.theme.PolliDimens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * DC [ConversationFragment] — reverse [RecyclerView], [PolliConversationAdapter] on `int[]`,
 * [StickyHeaderDecoration] for day pills, no scroll-idle work.
 */
@Composable
fun ChatFeedRecycler(
    viewModel: ChatViewModel,
    reloadGeneration: Int,
    contentGeneration: Int,
    headerClearance: Dp,
    feedBottomPadding: Dp,
    hazeState: HazeState,
    scrollController: ChatRecyclerController,
    playbackViewModel: PolliAudioPlaybackViewModel?,
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
    val configuration = LocalConfiguration.current
    val uiScale = prefs.uiScalePreset.factor
    val maxBubbleWidth =
        remember(configuration.screenWidthDp, uiScaleRevision) {
            val screenWidth = (configuration.screenWidthDp / uiScale).dp
            chatBubbleLaneMaxWidth(
                screenWidth = screenWidth,
                startGutter = PolliDimens.ChatRowPaddingH,
                endGutter = PolliDimens.ChatRowPaddingH,
            )
        }
    val topPadPx = remember(headerClearance, density) { with(density) { headerClearance.roundToPx() } }
    val bottomPadPx = remember(feedBottomPadding, density) { with(density) { feedBottomPadding.roundToPx() } }
    val hazeEnabled = BuildConfig.CHAT_HAZE_ENABLED

    val adapter =
        remember(maxBubbleWidth) {
            PolliConversationAdapter(
                viewModel = viewModel,
                maxBubbleWidth = maxBubbleWidth,
                playbackViewModel = playbackViewModel,
                onOpenMessageOverlay = onOpenMessageOverlay,
                onQuoteClick = onScrollToMessage,
            ).also { it.changeData(viewModel.adapterMessageIds()) }
        }

    val headerDecoration = remember { StickyHeaderDecoration(adapter) }

    var prevMsgCount by remember { mutableIntStateOf(0) }
    var appliedGeneration by remember { mutableIntStateOf(-1) }
    var appliedRowRefresh by remember { mutableIntStateOf(0) }
    var appliedUiScaleRevision by remember { mutableIntStateOf(uiScaleRevision) }

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
        val msgIds = viewModel.adapterMessageIds()
        val layoutManager = recycler.layoutManager as? PolliChatLayoutManager ?: return
        if (viewModel.pendingFirstLoadScroll) {
            applyStartingPosition(layoutManager, msgIds.size)
        }
        val preserveScroll = !viewModel.pendingFirstLoadScroll
        val anchor =
            if (preserveScroll) {
                ChatFeedScrollAnchor.capture(recycler, layoutManager, adapter.itemCount)
            } else {
                null
            }
        headerDecoration.clearHeaderCache()
        adapter.changeData(msgIds)
        adapter.updateChrome(viewModel.highlightId, viewModel.reactionPulse)
        if (anchor != null) {
            ChatFeedScrollAnchor.restore(layoutManager, anchor, adapter.itemCount)
        }
        appliedGeneration = reloadGeneration
        afterFeedSync(recycler, msgIds.size)
    }

    val feedModifier =
        Modifier.fillMaxSize().let { base ->
            if (hazeEnabled) base.hazeSource(state = hazeState) else base
        }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = feedModifier,
            factory = { context ->
                val msgIds = viewModel.adapterMessageIds()
                val layoutManager =
                    PolliChatLayoutManager(context).also {
                        applyStartingPosition(it, msgIds.size)
                    }
                adapter.changeData(msgIds)
                adapter.updateChrome(viewModel.highlightId, viewModel.reactionPulse)
                RecyclerView(context).apply {
                    this.layoutManager = layoutManager
                    itemAnimator = null
                    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                    clipToPadding = false
                    setItemViewCacheSize(24)
                    setPadding(0, topPadPx, 0, bottomPadPx)
                    this.adapter = adapter
                    addItemDecoration(headerDecoration)
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
                    afterFeedSync(this, msgIds.size)
                }
            },
            update = { recycler ->
                val prevBottomPad = recycler.paddingBottom
                recycler.setPadding(0, topPadPx, 0, bottomPadPx)
                if (bottomPadPx != prevBottomPad && scrollController.isAtChatBottom()) {
                    recycler.post {
                        (recycler.layoutManager as? LinearLayoutManager)?.scrollToPosition(0)
                    }
                }
                scrollController.recyclerView = recycler
                adapter.updateChrome(viewModel.highlightId, viewModel.reactionPulse)
                if (appliedRowRefresh != viewModel.rowRefreshToken) {
                    appliedRowRefresh = viewModel.rowRefreshToken
                    adapter.refreshMessage(viewModel.rowRefreshMsgId)
                }
                if (appliedUiScaleRevision != uiScaleRevision) {
                    appliedUiScaleRevision = uiScaleRevision
                    adapter.changeData(viewModel.adapterMessageIds())
                }
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
