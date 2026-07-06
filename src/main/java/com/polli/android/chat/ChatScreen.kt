package com.polli.android.chat

import com.polli.domain.model.chat.ChatActionContext
import com.polli.domain.model.chat.ChatMessage
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.polli.android.media.ChatMediaTabPanel
import com.polli.android.navigation.AppNav
import com.polli.android.theme.PolliColors
import com.polli.ui.components.ChatComingSoonTab
import com.polli.android.theme.PolliDimens
import com.polli.android.ui.AppInsets
import com.polli.android.ui.ChatFeedEdgeGradients
import com.polli.android.ui.rememberComposerChromeLayout
import com.polli.android.ui.rememberPolliHazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.audioplay.AudioPlaybackViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    chatTitle: String,
    chatSeed: String,
    chatId: Int,
    chatSession: ChatActionContext,
    isGroup: Boolean,
    isBroadcast: Boolean,
    onBack: () -> Unit,
    uiScaleRevision: Int = 0,
    playbackViewModel: AudioPlaybackViewModel? = null,
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
    pendingAttachment: PendingAttachment? = null,
    onClearAttachment: () -> Unit = {},
    onSendMessage: () -> Unit = { viewModel.send() },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tabs = remember(isGroup, isBroadcast) { tabsForChat(isGroup, isBroadcast) }
    val useGroupPager = tabs.size > 1
    var selectedTab by remember { mutableStateOf(ChatDetailTab.Chat) }
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOf(ChatDetailTab.Chat).coerceAtLeast(0),
        pageCount = { tabs.size },
    )
    val scrollController = remember { ChatRecyclerController() }
    val showScrollToBottom by scrollController.showScrollToBottom
    val headerClearance = if (isGroup && !isBroadcast) {
        PolliDimens.GroupHeaderClearance + AppInsets.statusBarTop()
    } else {
        AppInsets.statusBarTop() + 52.dp
    }

    LaunchedEffect(selectedTab, useGroupPager) {
        if (!useGroupPager) return@LaunchedEffect
        val idx = tabs.indexOf(selectedTab).coerceAtLeast(0)
        if (pagerState.currentPage == idx) return@LaunchedEffect
        pagerState.animateScrollToPage(idx)
    }

    val actionExecutor = remember(chatId, chatSession) {
        MessageActionExecutor(
            context = context,
            chatId = chatId,
            onReply = { viewModel.setReply(it) },
            onEdit = { viewModel.beginEdit(it) },
            onDeleted = viewModel::reload,
        )
    }
    val hazeState = rememberPolliHazeState()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val keyboardVisible = imeBottom > 0.dp

    val scrollToMessage: (Int) -> Unit = { msgId ->
        viewModel.jumpToMessage(msgId) { displayIndex ->
            scrollController.scrollMaybeSmoothToDisplayIndex(displayIndex)
        }
    }

    val openMessageOverlay: (ChatMessage, androidx.compose.ui.geometry.Offset) -> Unit = { message, tap ->
        viewModel.showOverlay(message, tap.x, tap.y)
    }

    val composerChrome = rememberComposerChromeLayout()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .onGloballyPositioned { composerChrome.onRootPositioned(it) },
    ) {
        if (useGroupPager) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
            ) { page ->
                ChatTabContent(
                    tab = tabs[page],
                    viewModel = viewModel,
                    scrollController = scrollController,
                    headerClearance = headerClearance,
                    feedBottomPadding = composerChrome.feedBottomPadding,
                    hazeState = hazeState,
                    uiScaleRevision = uiScaleRevision,
                    playbackViewModel = playbackViewModel,
                    chatId = chatId,
                    onOpenMessageOverlay = openMessageOverlay,
                    onScrollToMessage = scrollToMessage,
                )
            }
        } else {
            ChatFeedPage(
                viewModel = viewModel,
                scrollController = scrollController,
                headerClearance = headerClearance,
                feedBottomPadding = composerChrome.feedBottomPadding,
                hazeState = hazeState,
                uiScaleRevision = uiScaleRevision,
                playbackViewModel = playbackViewModel,
                onOpenMessageOverlay = openMessageOverlay,
                onScrollToMessage = scrollToMessage,
            )
        }

        if (selectedTab == ChatDetailTab.Chat) {
            var voiceLockVisible by remember { mutableStateOf(false) }
            var voiceLockDragY by remember { mutableFloatStateOf(0f) }
            val composerDockHeight = composerChrome.dockHeight

            ChatFeedEdgeGradients(
                modifier = Modifier.fillMaxSize(),
                topChromeClearance = headerClearance,
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
                onSend = onSendMessage,
                replyQuote = viewModel.replyTo?.toReplyQuote(),
                onClearQuote = { viewModel.setReply(null) },
                pendingAttachment = pendingAttachment,
                onClearAttachment = onClearAttachment,
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
                            end = PolliDimens.HomeBarPadding - 2.dp,
                            bottom = composerDockHeight + PolliDimens.ChatScrollFabGapAboveComposer,
                        ),
                )
            }

            ChatScrollToBottomButton(
                visible = showScrollToBottom && !voiceLockVisible,
                unreadCount = viewModel.unreadBelowCount,
                contentDescription = stringResource(R.string.menu_scroll_to_bottom),
                hazeState = hazeState,
                onClick = {
                    scrollController.scrollToBottom(
                        animated = (scrollController.recyclerView?.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)
                            ?.findFirstVisibleItemPosition()
                            ?.let { it < 50 } == true,
                    )
                    viewModel.onScrolledToBottom()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .imePadding()
                    .padding(
                        end = PolliDimens.HomeBarPadding - 2.dp,
                        bottom = composerDockHeight + PolliDimens.ChatScrollFabGapAboveComposer,
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
    scrollController: ChatRecyclerController,
    headerClearance: androidx.compose.ui.unit.Dp,
    feedBottomPadding: androidx.compose.ui.unit.Dp,
    hazeState: dev.chrisbanes.haze.HazeState,
    uiScaleRevision: Int,
    playbackViewModel: AudioPlaybackViewModel?,
    onOpenMessageOverlay: (ChatMessage, androidx.compose.ui.geometry.Offset) -> Unit,
    onScrollToMessage: (Int) -> Unit,
) {
    ChatFeedRecycler(
        viewModel = viewModel,
        reloadGeneration = viewModel.reloadGeneration,
        headerClearance = headerClearance,
        feedBottomPadding = feedBottomPadding,
        hazeState = hazeState,
        scrollController = scrollController,
        playbackViewModel = playbackViewModel,
        uiScaleRevision = uiScaleRevision,
        onOpenMessageOverlay = onOpenMessageOverlay,
        onScrollToMessage = onScrollToMessage,
        onFirstLoadScrollDone = {
            viewModel.highlightScrollIndex.takeIf { it >= 0 }?.let { hi ->
                viewModel.messageIdAtDisplayIndex(hi)?.let { id ->
                    viewModel.highlightMessage(id)
                }
            }
            viewModel.clearFirstLoadScroll()
        },
        onScrolledToBottom = viewModel::onScrolledToBottom,
        onUnreadBelow = viewModel::addUnreadBelow,
        consumeScrollToBottomOnReload = viewModel::consumeScrollToBottomOnReload,
    )
}

@Composable
private fun ChatTabContent(
    tab: ChatDetailTab,
    viewModel: ChatViewModel,
    scrollController: ChatRecyclerController,
    headerClearance: androidx.compose.ui.unit.Dp,
    feedBottomPadding: androidx.compose.ui.unit.Dp,
    hazeState: dev.chrisbanes.haze.HazeState,
    uiScaleRevision: Int,
    playbackViewModel: AudioPlaybackViewModel?,
    chatId: Int,
    onOpenMessageOverlay: (ChatMessage, androidx.compose.ui.geometry.Offset) -> Unit,
    onScrollToMessage: (Int) -> Unit,
) {
    val context = LocalContext.current
    when (tab) {
        ChatDetailTab.Search -> ChatComingSoonTab("Search")
        ChatDetailTab.Activity -> ChatComingSoonTab("Activity")
        ChatDetailTab.Chat -> ChatFeedPage(
            viewModel = viewModel,
            scrollController = scrollController,
            headerClearance = headerClearance,
            feedBottomPadding = feedBottomPadding,
            hazeState = hazeState,
            uiScaleRevision = uiScaleRevision,
            playbackViewModel = playbackViewModel,
            onOpenMessageOverlay = onOpenMessageOverlay,
            onScrollToMessage = onScrollToMessage,
        )
        ChatDetailTab.Files -> ChatMediaTabPanel(
            chatId = chatId,
            topPadding = headerClearance,
            onOpenMessage = { msgId -> AppNav.openMediaPreview(context, msgId) },
        )
        ChatDetailTab.Apps -> ChatComingSoonTab("Apps")
        ChatDetailTab.Tasks -> ChatComingSoonTab("Tasks")
        ChatDetailTab.Docs -> ChatComingSoonTab("Docs")
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
