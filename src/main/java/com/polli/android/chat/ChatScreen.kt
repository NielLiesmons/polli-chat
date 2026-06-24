package com.polli.android.chat

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.foundation.layout.absoluteOffset
import com.polli.android.ui.LabAvatar
import com.polli.android.ui.rememberLazyListShowTopFadeDerived
import com.polli.android.ui.rememberPolliHazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.thoughtcrime.securesms.AllMediaActivity
import org.thoughtcrime.securesms.R

internal val LocalIncomingAvatarSticky = staticCompositionLocalOf<IncomingAvatarStickyState?> { null }
internal val LocalChatDisplayItems = staticCompositionLocalOf<List<FeedItem>> { emptyList() }

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
    val composerClearance = AppInsets.chatComposerClearance()
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

    val scrollToMessage: (Int, (() -> Unit)?) -> Unit = { msgId, onComplete ->
        viewModel.jumpToMessage(msgId) { displayIndex ->
            scope.launch {
                listState.scrollToQuoteTarget(displayIndex)
                onComplete?.invoke()
            }
        }
    }

    val openMessageOverlay: (ChatMessage) -> Unit = { message ->
        scrollToMessage(message.id) { viewModel.showOverlay(message) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.Black),
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
                    composerClearance = composerClearance,
                    hazeState = hazeState,
                    onOpenMessageOverlay = openMessageOverlay,
                    onScrollToMessage = { msgId -> scrollToMessage(msgId, null) },
                )
                ChatDetailTab.Apps,
                ChatDetailTab.Files,
                -> ChatTabPlaceholder("Media tab") {
                    context.startActivity(
                        Intent(context, AllMediaActivity::class.java).apply {
                            putExtra(AllMediaActivity.CHAT_ID_EXTRA, chatId)
                        },
                    )
                }
                ChatDetailTab.Tasks -> ChatTabPlaceholder("Tasks — coming soon")
                ChatDetailTab.Docs -> ChatTabPlaceholder("Docs — coming soon")
            }
        }

        if (selectedTab == ChatDetailTab.Chat) {
            ChatFeedEdgeGradients(
                modifier = Modifier.fillMaxSize(),
                showTopFade = showTopFade,
                hasGroupHeader = isGroup && !isBroadcast,
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

        if (selectedTab == ChatDetailTab.Chat) {
            val density = LocalDensity.current
            var composerDockHeight by remember { mutableStateOf(0.dp) }

            ChatComposerDock(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .onGloballyPositioned { coords ->
                        composerDockHeight = with(density) { coords.size.height.toDp() }
                    },
                value = viewModel.draft,
                onValueChange = viewModel::updateDraft,
                onSend = viewModel::send,
                replyQuote = viewModel.replyTo?.toReplyQuote(),
                onClearQuote = { viewModel.setReply(null) },
                onAttachClick = onAttachClick,
                onVoiceSent = onVoiceSent,
                hazeState = hazeState,
            )

            ChatScrollToBottomButton(
                visible = showScrollToBottom,
                contentDescription = stringResource(R.string.menu_scroll_to_bottom),
                hazeState = hazeState,
                onClick = {
                    scope.launch {
                        listState.scrollToChatBottom(
                            animated = listState.firstVisibleItemIndex < 50,
                        )
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
    composerClearance: androidx.compose.ui.unit.Dp,
    hazeState: dev.chrisbanes.haze.HazeState,
    onOpenMessageOverlay: (ChatMessage) -> Unit,
    onScrollToMessage: (Int) -> Unit,
) {
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val displayItems = remember(viewModel.feedItems) { viewModel.feedItems.asReversed() }
    var prevItemCount by remember { mutableIntStateOf(0) }
    val avatarStickyState = remember { IncomingAvatarStickyState() }
    val density = LocalDensity.current
    val bottomPadPx = with(density) { (composerClearance + imeBottom).toPx() }

    LaunchedEffect(viewModel.reloadGeneration, displayItems.size) {
        if (displayItems.isEmpty()) {
            prevItemCount = 0
            return@LaunchedEffect
        }

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
            }
            listState.isAtChatBottom() -> {
                listState.scrollToItem(0)
            }
            listState.firstVisibleItemIndex > 0 -> {
                val delta = displayItems.size - prevItemCount
                var newIndex = listState.firstVisibleItemIndex + delta
                if (newIndex < 0) newIndex = 0
                if (newIndex > displayItems.lastIndex) newIndex = displayItems.lastIndex
                listState.scrollToItem(newIndex, scrollOffset = listState.firstVisibleItemScrollOffset)
            }
        }
        prevItemCount = displayItems.size
    }

    var feedRootTopLeft by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
        val stickyOverlay = avatarStickyState.overlay(listState, displayItems)
        CompositionLocalProvider(
            LocalIncomingAvatarSticky provides avatarStickyState,
            LocalChatDisplayItems provides displayItems,
        ) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        val root = coords.boundsInRoot()
                        feedRootTopLeft = androidx.compose.ui.geometry.Offset(root.left, root.top)
                        avatarStickyState.updateFeedAnchor(
                            rootLeft = root.left,
                            stickyBottom = root.bottom - bottomPadPx,
                        )
                    },
                contentPadding = PaddingValues(
                    top = headerClearance,
                    bottom = composerClearance + imeBottom,
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
                        listState = listState,
                        onQuoteClick = onScrollToMessage,
                        onOpenMessageOverlay = onOpenMessageOverlay,
                    )
                }
            }
        }
        stickyOverlay?.let { overlay ->
            LabAvatar(
                name = overlay.authorName,
                seed = overlay.authorKey,
                size = LabDimens.ChatAvatarSize,
                contactId = overlay.authorId,
                modifier = Modifier
                    .zIndex(2f)
                    .absoluteOffset(
                        x = with(density) { (overlay.rootTopLeftX - feedRootTopLeft.x).toDp() },
                        y = with(density) { (overlay.rootTopLeftY - feedRootTopLeft.y).toDp() },
                    ),
            )
        }
    }
}

@Composable
private fun ChatFeedItem(
    item: FeedItem,
    viewModel: ChatViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onQuoteClick: (Int) -> Unit,
    onOpenMessageOverlay: (ChatMessage) -> Unit,
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
                    onSwipeOptions = { onOpenMessageOverlay(msg) },
                    onClick = { onOpenMessageOverlay(msg) },
                    onQuoteClick = onQuoteClick,
                )
            } else {
                SingleIncomingMessageRow(
                    message = msg,
                    layout = item.layout,
                    highlighted = viewModel.highlightId == msg.id,
                    reactionReloadKey = viewModel.reloadGeneration,
                    pulseEmoji = pulseEmoji,
                    listState = listState,
                    onSwipeReply = { viewModel.setReply(msg) },
                    onSwipeOptions = { onOpenMessageOverlay(msg) },
                    onClick = { onOpenMessageOverlay(msg) },
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
