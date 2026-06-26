package com.polli.android.chat

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.polli.core.chat.ChatDetailTab
import com.polli.core.chat.tabsForChat
import com.polli.android.navigation.AppNav
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.ui.AppInsets
import com.polli.android.ui.ChatFeedEdgeGradients
import com.polli.android.ui.rememberComposerChromeLayout
import com.polli.android.ui.rememberLazyListShowTopFadeDerived
import com.polli.android.ui.rememberPolliHazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.thoughtcrime.securesms.R

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    chatTitle: String,
    chatSeed: String,
    chatId: Int,
    isGroup: Boolean,
    isBroadcast: Boolean,
    onBack: () -> Unit,
    showAttachModal: Boolean = false,
    onDismissAttachModal: () -> Unit = {},
    onAttachClick: (() -> Unit)? = null,
    onBrowseFiles: () -> Unit = {},
    onPickGallery: () -> Unit = {},
    onCamera: () -> Unit = {},
    onPickVideo: () -> Unit = {},
    onPickContact: () -> Unit = {},
    onPickLocation: () -> Unit = {},
    onVoiceSent: ((android.net.Uri, Long) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tabs = remember(isGroup, isBroadcast) { tabsForChat(isGroup, isBroadcast) }
    var selectedTab by remember { mutableStateOf(ChatDetailTab.Chat) }
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOf(ChatDetailTab.Chat).coerceAtLeast(0),
        pageCount = { tabs.size },
    )
    val listState = rememberLazyListState()
    val showTopFade by rememberLazyListShowTopFadeDerived(listState)
    val showScrollToBottom by rememberShowChatScrollToBottom(listState)
    val headerClearance = if (isGroup && !isBroadcast) {
        LabDimens.GroupHeaderClearance + AppInsets.statusBarTop()
    } else {
        AppInsets.statusBarTop() + 52.dp
    }

    LaunchedEffect(selectedTab) {
        val idx = tabs.indexOf(selectedTab).coerceAtLeast(0)
        pagerState.animateScrollToPage(idx)
    }

    val actionExecutor = remember(chatId) {
        MessageActionExecutor(
            context = context,
            chatId = chatId,
            onReply = { dcMsg ->
                viewModel.messages.find { it.id == dcMsg.id }?.let { viewModel.setReply(it) }
            },
            onEdit = viewModel::beginEdit,
            onDeleted = viewModel::reload,
        )
    }
    val hazeState = rememberPolliHazeState()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val keyboardVisible = imeBottom > 0.dp

    val scrollToMessage: (Int) -> Unit = { msgId ->
        viewModel.jumpToMessage(msgId) { displayIndex ->
            scope.launch {
                listState.scrollMaybeSmoothToDisplayIndex(displayIndex)
            }
        }
    }

    val openMessageOverlay: (ChatMessage, androidx.compose.ui.geometry.Offset) -> Unit = { message, tap ->
        viewModel.showOverlay(message, tap.x, tap.y)
    }

    val composerChrome = rememberComposerChromeLayout()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.Black)
            .onGloballyPositioned { composerChrome.onRootPositioned(it) },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
        ) { page ->
            when (tabs[page]) {
                ChatDetailTab.Search -> ChatTabPlaceholder("Search — coming soon")
                ChatDetailTab.Activity -> ChatTabPlaceholder("Activity — coming soon")
                ChatDetailTab.Chat -> ChatFeedPage(
                    viewModel = viewModel,
                    listState = listState,
                    headerClearance = headerClearance,
                    feedBottomPadding = composerChrome.feedBottomPadding,
                    hazeState = hazeState,
                    onOpenMessageOverlay = openMessageOverlay,
                    onScrollToMessage = scrollToMessage,
                )
                ChatDetailTab.Apps,
                ChatDetailTab.Files,
                -> ChatTabPlaceholder("Media tab") {
                    AppNav.openAllMedia(context, chatId)
                }
                ChatDetailTab.Tasks -> ChatTabPlaceholder("Tasks — coming soon")
                ChatDetailTab.Docs -> ChatTabPlaceholder("Docs — coming soon")
            }
        }

        if (selectedTab == ChatDetailTab.Chat) {
            var voiceLockVisible by remember { mutableStateOf(false) }
            var voiceLockDragY by remember { mutableFloatStateOf(0f) }
            val composerDockHeight = composerChrome.dockHeight

            ChatFeedEdgeGradients(
                modifier = Modifier.fillMaxSize(),
                showTopFade = showTopFade,
                hasGroupHeader = isGroup && !isBroadcast,
                bottomChromeInset = composerChrome.bottomChromeInset,
            )

            ChatComposerDock(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .onGloballyPositioned { composerChrome.onComposerPositioned(it) },
                value = viewModel.draft,
                onValueChange = viewModel::updateDraft,
                onSend = viewModel::send,
                replyQuote = viewModel.replyTo?.toReplyQuote(),
                onClearQuote = { viewModel.setReply(null) },
                onAttachClick = onAttachClick,
                onVoiceSent = onVoiceSent,
                onVoiceLockOverlayChange = { visible, dragUpPx ->
                    voiceLockVisible = visible
                    voiceLockDragY = dragUpPx
                },
                hazeState = hazeState,
            )

            if (voiceLockVisible) {
                VoiceLockPill(
                    visible = true,
                    dragUpPx = voiceLockDragY,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .imePadding()
                        .padding(
                            end = LabDimens.HomeBarPadding - 2.dp,
                            bottom = composerDockHeight + LabDimens.ChatScrollFabGapAboveComposer,
                        ),
                )
            }

            ChatScrollToBottomButton(
                visible = showScrollToBottom && !voiceLockVisible,
                unreadCount = viewModel.unreadBelowCount,
                contentDescription = stringResource(R.string.menu_scroll_to_bottom),
                hazeState = hazeState,
                onClick = {
                    scope.launch {
                        listState.scrollToChatBottom(
                            animated = listState.firstVisibleItemIndex < 50,
                        )
                        viewModel.onScrolledToBottom()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .imePadding()
                    .padding(
                        end = LabDimens.HomeBarPadding - 2.dp,
                        bottom = composerDockHeight + LabDimens.ChatScrollFabGapAboveComposer,
                    ),
            )
        }

        if (isGroup && !isBroadcast) {
            GroupHeaderChrome(
                chatTitle = chatTitle,
                chatId = chatId,
                isGroup = isGroup,
                isBroadcast = isBroadcast,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onBack = onBack,
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        } else {
            SimpleChatHeader(
                title = chatTitle,
                seed = chatSeed,
                chatId = chatId,
                onBack = onBack,
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        if (showAttachModal) {
            ComposerAttachModal(
                onClose = onDismissAttachModal,
                onGallery = onPickGallery,
                onCamera = onCamera,
                onBrowse = onBrowseFiles,
                onVideo = onPickVideo,
                onContact = onPickContact,
                onLocation = onPickLocation,
                hazeState = hazeState,
            )
        }

        BubbleOverlayHost(
            anchor = viewModel.overlayAnchor,
            hazeState = hazeState,
            keyboardVisible = keyboardVisible,
            onDismiss = viewModel::dismissOverlay,
            onReaction = { emoji ->
                val msgId = viewModel.overlayAnchor?.message?.id ?: return@BubbleOverlayHost
                viewModel.dismissOverlay()
                viewModel.sendReaction(msgId, emoji)
            },
            onReply = {
                val msg = viewModel.overlayAnchor?.message ?: return@BubbleOverlayHost
                viewModel.dismissOverlay()
                viewModel.setReply(msg)
            },
            onDelete = {
                val msgId = viewModel.overlayAnchor?.message?.id ?: return@BubbleOverlayHost
                viewModel.dismissOverlay()
                viewModel.deleteMessage(msgId)
            },
        )
    }
}

@Composable
private fun ChatFeedPage(
    viewModel: ChatViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState,
    headerClearance: androidx.compose.ui.unit.Dp,
    feedBottomPadding: androidx.compose.ui.unit.Dp,
    hazeState: dev.chrisbanes.haze.HazeState,
    onOpenMessageOverlay: (ChatMessage, androidx.compose.ui.geometry.Offset) -> Unit,
    onScrollToMessage: (Int) -> Unit,
) {
    val displayItems = remember(viewModel.feedItems) { viewModel.feedItems.asReversed() }
    var prevMsgCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isAtChatBottom() }.collect { atBottom ->
            if (atBottom) viewModel.onScrolledToBottom()
        }
    }

    LaunchedEffect(viewModel.reloadGeneration, displayItems.size) {
        if (displayItems.isEmpty()) {
            prevMsgCount = 0
            return@LaunchedEffect
        }

        val msgCount = displayItems.count { it is FeedItem.Message }
        val msgDelta = msgCount - prevMsgCount

        when {
            viewModel.pendingFirstLoadScroll -> {
                val target = viewModel.initialScrollIndex.coerceIn(0, displayItems.lastIndex)
                listState.scrollToItem(target)
                viewModel.highlightScrollIndex.takeIf { it >= 0 }?.let { hi ->
                    viewModel.messageIdAtDisplayIndex(hi)?.let { id ->
                        viewModel.highlightMessage(id)
                    }
                }
                viewModel.clearFirstLoadScroll()
                viewModel.onScrolledToBottom()
            }
            viewModel.consumeScrollToBottomOnReload() -> {
                listState.scrollToChatBottom(
                    animated = listState.firstVisibleItemIndex < 50,
                )
                viewModel.onScrolledToBottom()
            }
            listState.isAtChatBottom() -> {
                if (msgDelta > 0) {
                    listState.scrollToChatBottom(
                        animated = listState.firstVisibleItemIndex < 50,
                    )
                }
                viewModel.onScrolledToBottom()
            }
            msgDelta > 0 -> {
                viewModel.addUnreadBelow(msgDelta)
            }
        }
        prevMsgCount = msgCount
    }

    Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = headerClearance,
                bottom = feedBottomPadding,
            ),
        ) {
            itemsIndexed(
                items = displayItems,
                key = { _, item ->
                    when (item) {
                        is FeedItem.DayMarker -> "day-${item.label}"
                        is FeedItem.Message -> "msg-${item.message.id}"
                    }
                },
            ) { _, item ->
                ChatFeedItem(
                    item = item,
                    viewModel = viewModel,
                    onQuoteClick = onScrollToMessage,
                    onOpenMessageOverlay = onOpenMessageOverlay,
                )
            }
        }
    }
}

@Composable
private fun ChatFeedItem(
    item: FeedItem,
    viewModel: ChatViewModel,
    onQuoteClick: (Int) -> Unit,
    onOpenMessageOverlay: (ChatMessage, androidx.compose.ui.geometry.Offset) -> Unit,
) {
    when (item) {
        is FeedItem.DayMarker -> {
            Text(
                text = item.label,
                color = LabColors.White33,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
        is FeedItem.Message -> {
            val msg = item.message
            val pulseEmoji = viewModel.reactionPulse?.takeIf { it.msgId == msg.id }?.emoji
            if (msg.isOutgoing) {
                OutgoingMessageRow(
                    message = msg,
                    layout = item.layout,
                    highlighted = viewModel.highlightId == msg.id,
                    reactionReloadKey = viewModel.reloadGeneration,
                    pulseEmoji = pulseEmoji,
                    onSwipeReply = { viewModel.setReply(msg) },
                    onClick = { tap -> onOpenMessageOverlay(msg, tap) },
                    onQuoteClick = onQuoteClick,
                )
            } else {
                SingleIncomingMessageRow(
                    message = msg,
                    layout = item.layout,
                    highlighted = viewModel.highlightId == msg.id,
                    reactionReloadKey = viewModel.reloadGeneration,
                    pulseEmoji = pulseEmoji,
                    onSwipeReply = { viewModel.setReply(msg) },
                    onClick = { tap -> onOpenMessageOverlay(msg, tap) },
                    onQuoteClick = onQuoteClick,
                )
            }
        }
    }
}

@Composable
private fun SimpleChatHeader(
    title: String,
    seed: String,
    chatId: Int,
    onBack: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    modifier: Modifier = Modifier,
) {
    ChatHeaderTitleRow(
        title = title,
        chatId = chatId,
        chatSeed = seed,
        onBack = onBack,
        hazeState = hazeState,
        modifier = modifier,
    )
}

@Composable
private fun ChatTabPlaceholder(message: String, onOpen: (() -> Unit)? = null) {
    LaunchedEffect(Unit) { onOpen?.invoke() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = LabColors.White33)
    }
}
