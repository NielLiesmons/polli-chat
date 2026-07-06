package com.polli.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import com.polli.ui.components.polliClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.polli.domain.model.InboxItem
import com.polli.ui.components.PolliIcon
import com.polli.ui.components.PolliIconName
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.PolliDimens
import com.polli.ui.theme.accent
import com.polli.ui.theme.AppInsets
import com.polli.ui.components.ArchiveLinkRow
import com.polli.ui.components.ChatInboxCard
import com.polli.ui.components.rememberLazyListShowTopFadeDerived
import com.polli.ui.components.PolliScreenScrim
import com.polli.ui.components.rememberPolliHazeState
import com.polli.ui.components.scrollFadeMask
import com.polli.ui.components.HomeBackHandler
import com.polli.ui.components.ProfileAvatar
import com.polli.ui.components.SelfAvatar
import com.polli.ui.home.HomeNote
import com.polli.ui.home.HomeSearchPanelBody
import com.polli.ui.home.formatHomeTabUnreadCount
import com.polli.ui.home.totalUnreadMessages
import com.polli.ui.home.HomeSearchPanelHeightMeasurer
import com.polli.ui.home.HomeSearchActionButton
import com.polli.ui.home.HomeSearchPillSurface
import com.polli.ui.home.NotesTab
import com.polli.ui.home.SigilsTab
import com.polli.ui.home.StoryLaunchBounds
import com.polli.ui.home.StoryRingEntry
import com.polli.ui.home.StoryRingLogic
import com.polli.ui.home.StoryRingStyle
import com.polli.ui.home.StorySession
import com.polli.ui.home.rememberArchiveLinkState
import com.polli.ui.home.rememberInboxItems
import com.polli.domain.repository.ChatRepository
import com.polli.core.chat.ChatCategory
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.unit.Dp

private val SearchExpandEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private const val SearchExpandDurationMs = 380

enum class HomeTab { Home, Notes, Sigils }

@Composable
fun HomeScreen(
    profileName: String,
    profileSeed: String,
    chatRepository: ChatRepository,
    archivedChatsTitle: String = "Archived chats",
    notes: List<HomeNote> = emptyList(),
    storyRingLoader: ((List<InboxItem>, Long) -> List<StoryRingEntry>)? = null,
    homeEmptyHint: String = "No chats yet. Your spaces and mail appear here.",
    onProfileClick: () -> Unit,
    onPlusClick: () -> Unit,
    onChatClick: (Int) -> Unit,
    onChannelClick: (Int) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onNewNote: () -> Unit = {},
    onOpenNote: (Int) -> Unit = {},
    shareRelayTitle: String? = null,
    storiesOverlay: (@Composable (StorySession, () -> Unit) -> Unit)? = null,
    chatAvatar: @Composable (InboxItem, Dp) -> Unit = { item, size ->
        ProfileAvatar(name = item.name, seed = item.colorSeed, size = size)
    },
    selfAvatar: @Composable (String, Dp, () -> Unit) -> Unit = { name, size, onClick ->
        SelfAvatar(name = name, size = size, onClick = onClick)
    },
) {
    var searchPanelOpen by remember { mutableStateOf(false) }
    var storySession by remember { mutableStateOf<StorySession?>(null) }
    var storyRingRefreshKey by remember { mutableIntStateOf(0) }
    var dragExpandProgress by remember { mutableFloatStateOf(0f) }
    var isDraggingExpand by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(HomeTab.Home) }
    val listState = rememberLazyListState()
    val showTopFade by rememberLazyListShowTopFadeDerived(listState)
    val focusRequester = remember { FocusRequester() }
    val hazeState = rememberPolliHazeState()

    val expandTarget = when {
        isDraggingExpand -> dragExpandProgress
        searchPanelOpen -> 1f
        else -> 0f
    }
    val expandProgress by animateFloatAsState(
        targetValue = expandTarget,
        animationSpec = if (isDraggingExpand) {
            snap()
        } else {
            tween(durationMillis = SearchExpandDurationMs, easing = SearchExpandEasing)
        },
        label = "searchExpand",
    )

    val loadedItems = rememberInboxItems(chatRepository, query)
    val loadedChannels = remember(loadedItems) {
        loadedItems.filter { it.category == ChatCategory.Channel }
    }
    val nowSec = remember { System.currentTimeMillis() / 1000 }
    val storyRingEntries =
        storyRingLoader?.invoke(loadedChannels, nowSec)
            ?: StoryRingLogic.buildEntries(loadedChannels, nowSec)
    val archiveLink = rememberArchiveLinkState(chatRepository)
    val homeItems =
        remember(loadedItems) {
            loadedItems.filter {
                it.category == ChatCategory.Space || it.category == ChatCategory.Mail
            }
        }
    val homeUnreadTotal = remember(homeItems) { totalUnreadMessages(homeItems) }

    fun openSearchPanel() {
        isDraggingExpand = false
        searchPanelOpen = true
    }

    fun closeSearchPanel() {
        isDraggingExpand = false
        searchPanelOpen = false
        dragExpandProgress = 0f
        query = ""
        onSearch("")
    }

    fun snapSearchPanelOpen() {
        isDraggingExpand = false
        searchPanelOpen = true
    }

    LaunchedEffect(searchPanelOpen) {
        if (searchPanelOpen) {
            focusRequester.requestFocus()
        }
    }

    HomeBackHandler(enabled = searchPanelOpen || expandProgress > 0.05f) {
        closeSearchPanel()
    }

    val density = LocalDensity.current
    val expandDragDistancePx = with(density) { PolliDimens.HomeSearchExpandDragDistance.toPx() }
    val snapThreshold = PolliDimens.HomeSearchExpandSnapThreshold
    val expandNestedScroll = remember(listState, searchPanelOpen, snapThreshold) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (searchPanelOpen) return Offset.Zero
                val atTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                if (!atTop || available.y <= 0f) return Offset.Zero

                isDraggingExpand = true
                val damped = 1f - dragExpandProgress * 0.45f
                val delta = (available.y / expandDragDistancePx) * damped
                dragExpandProgress = (dragExpandProgress + delta).coerceIn(0f, 1f)

                if (dragExpandProgress >= snapThreshold) {
                    snapSearchPanelOpen()
                }
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!isDraggingExpand) return Velocity.Zero
                isDraggingExpand = false
                if (dragExpandProgress >= snapThreshold) {
                    snapSearchPanelOpen()
                } else {
                    searchPanelOpen = false
                    dragExpandProgress = 0f
                }
                return Velocity.Zero
            }
        }
    }

    val feedAlpha = (1f - expandProgress * 0.92f).coerceIn(0f, 1f)
    val chromeAlpha = (1f - expandProgress * 0.88f).coerceIn(0f, 1f)
    val barTopInset = (PolliDimens.HomeProfileSize - PolliDimens.HomeBarHeight) / 2
    val searchExpanded = searchPanelOpen || expandProgress > 0.05f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .padding(top = AppInsets.statusBarTop()),
    ) {
        var searchPanelBodyHeight by remember {
            mutableStateOf(PolliDimens.HomeSearchPanelExpandedHeight - PolliDimens.HomeBarHeight)
        }
        HomeSearchPanelHeightMeasurer { measured ->
            if (measured > 0.dp) searchPanelBodyHeight = measured
        }
        val expandedPanelHeight = PolliDimens.HomeBarHeight + searchPanelBodyHeight
        val panelHeight = lerp(PolliDimens.HomeBarHeight, expandedPanelHeight, expandProgress)
        val headerBlockHeight = PolliDimens.HomeBarVerticalPad + maxOf(
            PolliDimens.HomeProfileSize,
            barTopInset + panelHeight,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState),
        ) {
            Spacer(modifier = Modifier.height(headerBlockHeight))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(chromeAlpha),
            ) {
                shareRelayTitle?.let { title ->
                    Text(
                        text = title,
                        color = PolliColors.White66,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PolliDimens.HomeBarPadding)
                            .padding(bottom = 8.dp),
                    )
                }
                if (expandProgress < 0.92f) {
                    Spacer(modifier = Modifier.height(PolliDimens.HomeChromeGap))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (storyRingEntries.isNotEmpty()) {
                            ChannelStoriesRow(
                                entries = storyRingEntries,
                                chatAvatar = chatAvatar,
                                onSelect = { entry, bounds ->
                                    when (entry.style) {
                                        StoryRingStyle.Stale -> onChannelClick(entry.channel.chatId)
                                        else -> {
                                            val ids = StoryRingLogic.storyChannelIds(storyRingEntries)
                                            if (storiesOverlay != null) {
                                                storySession =
                                                    StorySession(
                                                        channelId = entry.channel.chatId,
                                                        channelIds = ids,
                                                        launchBounds = bounds,
                                                    )
                                            } else {
                                                onChannelClick(entry.channel.chatId)
                                            }
                                        }
                                    }
                                },
                            )
                            Spacer(modifier = Modifier.height(PolliDimens.HomeChromeGap))
                        }
                        TabRow(
                            active = tab,
                            onSelect = { tab = it },
                            homeUnreadCount = homeUnreadTotal,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(PolliDimens.TabSectionGap))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(PolliColors.Black)
                    .alpha(feedAlpha),
            ) {
                if (tab == HomeTab.Sigils) {
                    SigilsTab()
                } else if (tab == HomeTab.Notes) {
                    NotesTab(
                        notes = notes,
                        onNewNote = onNewNote,
                        onOpenNote = onOpenNote,
                    )
                } else {
                    val filtered = homeItems
                    val showArchiveRow = tab == HomeTab.Home && archiveLink.visible
                    val showFeed = filtered.isNotEmpty() || showArchiveRow

                    if (!showFeed) {
                        val channelCount = loadedItems.count { it.category == ChatCategory.Channel }
                        val hint =
                            when {
                                loadedItems.isEmpty() -> homeEmptyHint
                                channelCount > 0 ->
                                    "No chats yet. Broadcast channels and mailing lists appear in the story row above."
                                else -> homeEmptyHint
                            }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = hint,
                                color = PolliColors.White33,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(expandNestedScroll)
                                .scrollFadeMask(showTopFade),
                            contentPadding = PaddingValues(
                                start = PolliDimens.HomeBarPadding,
                                end = PolliDimens.HomeBarPadding,
                                bottom = AppInsets.navigationBarBottom() + PolliDimens.TabContentBottomPad,
                            ),
                            verticalArrangement = Arrangement.spacedBy(PolliDimens.TabSectionGap),
                        ) {
                            if (showArchiveRow) {
                                item(key = "archive-link") {
                                    ArchiveLinkRow(
                                        label = archivedChatsTitle,
                                        unreadCount = archiveLink.unreadCount,
                                        onClick = onArchiveClick,
                                    )
                                }
                            }
                            items(filtered, key = { it.chatId }) { item ->
                                ChatInboxCard(
                                    item = item,
                                    onClick = { onChatClick(item.chatId) },
                                    nowSec = nowSec,
                                    avatar = {
                                        chatAvatar(item, PolliDimens.AvatarSize)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (searchExpanded) {
            PolliScreenScrim(onDismiss = { closeSearchPanel() })
        }

        HomeExpandableSearchHeader(
            expandProgress = expandProgress,
            panelHeight = panelHeight,
            profileName = profileName,
            query = query,
            searchPanelOpen = searchPanelOpen,
            hazeState = hazeState,
            onProfileClick = onProfileClick,
            onOpenSearch = { openSearchPanel() },
            onCloseSearch = { closeSearchPanel() },
            onQueryChange = { query = it; onSearch(it) },
            onPlusClick = onPlusClick,
            focusRequester = focusRequester,
            onCreateNote = {
                closeSearchPanel()
                onNewNote()
            },
            selfAvatar = selfAvatar,
        )

        storySession?.let { session ->
            storiesOverlay?.invoke(session) {
                storySession = null
                storyRingRefreshKey++
            }
        }
    }
}

@Composable
private fun HomeExpandableSearchHeader(
    expandProgress: Float,
    panelHeight: androidx.compose.ui.unit.Dp,
    profileName: String,
    query: String,
    searchPanelOpen: Boolean,
    hazeState: HazeState,
    onProfileClick: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onPlusClick: () -> Unit,
    focusRequester: FocusRequester,
    onCreateNote: () -> Unit,
    selfAvatar: @Composable (String, Dp, () -> Unit) -> Unit,
) {
    val cornerRadius = lerp(PolliDimens.HomeBarHeight / 2, 20.dp, expandProgress)
    val profileSlotWidth = lerp(
        PolliDimens.HomeProfileSize + PolliDimens.HomeProfileGap,
        0.dp,
        expandProgress,
    )
    val profileAlpha = (1f - expandProgress).coerceIn(0f, 1f)
    val barTopInset = (PolliDimens.HomeProfileSize - PolliDimens.HomeBarHeight) / 2
    val showField = searchPanelOpen || expandProgress > 0.08f

    Column(
        modifier = Modifier
            .padding(horizontal = PolliDimens.HomeBarPadding)
            .padding(top = PolliDimens.HomeBarVerticalPad),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(profileSlotWidth)
                    .height(PolliDimens.HomeProfileSize)
                    .clipToBounds()
                    .alpha(profileAlpha),
                contentAlignment = Alignment.CenterStart,
            ) {
                selfAvatar(
                    profileName,
                    PolliDimens.HomeProfileSize,
                    onProfileClick,
                )
            }

            HomeSearchPillSurface(
                cornerRadius = cornerRadius,
                hazeState = hazeState,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = barTopInset)
                    .height(panelHeight)
                    .clipToBounds(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PolliDimens.HomeBarHeight)
                            .then(
                                if (!showField) {
                                    Modifier.polliClickable(onClick = onOpenSearch)
                                } else {
                                    Modifier
                                },
                            )
                            .padding(
                                start = PolliDimens.HomePillInsetBeforeSearch,
                                end = 6.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PolliIcon(
                            PolliIconName.Search,
                            PolliDimens.HomeSearchGlyphSize,
                            PolliColors.White33,
                        )
                        if (showField) {
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = PolliDimens.HomeSearchGapAfterGlyph)
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.labelMedium.copy(
                                    color = PolliColors.White85,
                                ),
                                cursorBrush = SolidColor(PolliColors.White),
                                singleLine = true,
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (query.isEmpty()) {
                                            Text("Search", color = PolliColors.White16)
                                        }
                                        inner()
                                    }
                                },
                            )
                        } else {
                            Spacer(modifier = Modifier.width(PolliDimens.HomeSearchGapAfterGlyph))
                            Text(
                                "Search",
                                color = PolliColors.White16,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        HomeSearchActionButton(
                            expandProgress = expandProgress,
                            searchPanelOpen = searchPanelOpen,
                            onPlusClick = onPlusClick,
                            onCloseClick = onCloseSearch,
                        )
                    }

                    HomeSearchPanelBody(
                        expandProgress = expandProgress,
                        onRecentSelect = { term ->
                            onQueryChange(term)
                            onOpenSearch()
                        },
                        onCreateNote = onCreateNote,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelStoriesRow(
    entries: List<StoryRingEntry>,
    chatAvatar: @Composable (InboxItem, Dp) -> Unit,
    onSelect: (StoryRingEntry, StoryLaunchBounds) -> Unit,
) {
    if (entries.isEmpty()) return
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = PolliDimens.StoriesRowPaddingStart,
                    end = PolliDimens.StoriesRowPadding,
                )
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(PolliDimens.StoryRingSpacing),
    ) {
        entries.forEach { entry ->
            StoryRing(
                entry = entry,
                chatAvatar = chatAvatar,
                onClick = { bounds -> onSelect(entry, bounds) },
            )
        }
    }
}

@Composable
private fun StoryRing(
    entry: StoryRingEntry,
    chatAvatar: @Composable (InboxItem, Dp) -> Unit,
    onClick: (StoryLaunchBounds) -> Unit,
) {
    val channel = entry.channel
    val ringColor = when (entry.style) {
        StoryRingStyle.Unread -> accent().solid
        StoryRingStyle.ReadRecent -> accent().solid(0.16f)
        StoryRingStyle.Stale -> Color.Transparent
    }
    var launchBounds by remember(channel.chatId) { mutableStateOf<StoryLaunchBounds?>(null) }
    Box(
        modifier = Modifier
            .size(PolliDimens.StoryRingOuter)
            .onGloballyPositioned { coords ->
                val rect = coords.boundsInRoot()
                launchBounds = StoryLaunchBounds(
                    centerX = rect.left + rect.width / 2f,
                    centerY = rect.top + rect.height / 2f,
                    size = minOf(rect.width, rect.height),
                )
            }
            .clip(CircleShape)
            .background(ringColor)
            .padding(PolliDimens.StoryRingStroke)
            .polliClickable {
                launchBounds?.let(onClick)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(PolliColors.Black)
                .padding(PolliDimens.StoryRingGap),
            contentAlignment = Alignment.Center,
        ) {
            chatAvatar(channel, PolliDimens.StoryRingInner)
        }
    }
}

@Composable
private fun TabRow(
    active: HomeTab,
    onSelect: (HomeTab) -> Unit,
    homeUnreadCount: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = PolliDimens.HomeBarPadding),
        horizontalArrangement = Arrangement.spacedBy(PolliDimens.TabGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabPill(
            label = "Home",
            selected = active == HomeTab.Home,
            onClick = { onSelect(HomeTab.Home) },
            unreadCount = homeUnreadCount,
        )
        TabPill(
            label = "Notes",
            selected = active == HomeTab.Notes,
            onClick = { onSelect(HomeTab.Notes) },
        )
        TabPill(
            label = "Sigils",
            selected = active == HomeTab.Sigils,
            onClick = { onSelect(HomeTab.Sigils) },
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    unreadCount: Int = 0,
) {
    val height = if (selected) PolliDimens.TabButtonHeight else PolliDimens.TabButtonUnselectedHeight
    val hPadding = if (selected) PolliDimens.TabButtonHPadding else PolliDimens.TabButtonUnselectedHPadding
    val corner = if (selected) 17.dp else PolliDimens.TabButtonUnselectedCorner
    val fontSize = if (selected) 14.5.sp else 13.sp
    val bg =
        if (selected) {
            accent().gradientBrush(0.66f)
        } else {
            Brush.linearGradient(listOf(PolliColors.White8, PolliColors.White8))
        }
    val countLabel = formatHomeTabUnreadCount(unreadCount)
    val countColor =
        if (selected) {
            PolliColors.White.copy(alpha = 0.66f)
        } else {
            PolliColors.White66
        }
    Box(
        modifier =
            Modifier
                .height(height)
                .clip(RoundedCornerShape(corner))
                .background(bg)
                .polliClickable(onClick = onClick)
                .padding(horizontal = hPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                color = if (selected) PolliColors.White else PolliColors.White66,
                style =
                    TextStyle(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Medium,
                        lineHeight = fontSize,
                    ),
            )
            if (countLabel != null) {
                Text(
                    text = countLabel,
                    color = countColor,
                    style =
                        TextStyle(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Medium,
                            lineHeight = fontSize,
                        ),
                )
            }
        }
    }
}
