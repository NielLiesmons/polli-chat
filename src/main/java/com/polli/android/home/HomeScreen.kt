package com.polli.android.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.polli.android.bridge.InboxItem
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.theme.accent
import com.polli.android.ui.AppInsets
import com.polli.android.ui.LabAvatar
import com.polli.android.ui.SelfAvatar
import com.polli.android.ui.rememberLazyListShowTopFadeDerived
import com.polli.android.ui.PolliScreenScrim
import com.polli.android.ui.rememberPolliHazeState
import com.polli.android.stories.ChannelStoriesOverlay
import com.polli.android.stories.StoriesViewModel
import com.polli.android.stories.StoryLaunchBounds
import com.polli.android.stories.StorySession
import com.polli.android.ui.scrollFadeMask
import com.polli.core.chat.ChatCategory
import dev.chrisbanes.haze.hazeSource
import org.thoughtcrime.securesms.R

private val SearchExpandEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private const val SearchExpandDurationMs = 380

enum class HomeTab { Spaces, Mail, Sigils }

@Composable
fun HomeScreen(
    profileName: String,
    profileSeed: String,
    items: List<InboxItem>? = null,
    channels: List<InboxItem>? = null,
    storiesViewModel: StoriesViewModel? = null,
    spacesEmptyHint: String = "No spaces yet. Your group chats appear here.",
    mailEmptyHint: String = "No mail chats yet.",
    onProfileClick: () -> Unit,
    onPlusClick: () -> Unit,
    onChatClick: (Int) -> Unit,
    onChannelClick: (Int) -> Unit = {},
    onSearch: (String) -> Unit,
    onArchiveClick: () -> Unit = {},
) {
    var searchPanelOpen by remember { mutableStateOf(false) }
    var storySession by remember { mutableStateOf<StorySession?>(null) }
    var dragExpandProgress by remember { mutableFloatStateOf(0f) }
    var isDraggingExpand by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(HomeTab.Spaces) }
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

    val loadedItems = items ?: rememberInboxItems(query)
    val loadedChannels = channels ?: rememberChannels(loadedItems)
    val archiveLink = rememberArchiveLinkState()

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

    BackHandler(enabled = searchPanelOpen || expandProgress > 0.05f) {
        closeSearchPanel()
    }

    val density = LocalDensity.current
    val expandDragDistancePx = with(density) { LabDimens.HomeSearchExpandDragDistance.toPx() }
    val snapThreshold = LabDimens.HomeSearchExpandSnapThreshold
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
    val barTopInset = (LabDimens.HomeProfileSize - LabDimens.HomeBarHeight) / 2
    val searchExpanded = searchPanelOpen || expandProgress > 0.05f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.Black)
            .padding(top = AppInsets.statusBarTop()),
    ) {
        var searchPanelBodyHeight by remember {
            mutableStateOf(LabDimens.HomeSearchPanelExpandedHeight - LabDimens.HomeBarHeight)
        }
        val expandedPanelHeight = LabDimens.HomeBarHeight + searchPanelBodyHeight
        val panelHeight = lerp(LabDimens.HomeBarHeight, expandedPanelHeight, expandProgress)
        val headerBlockHeight = LabDimens.HomeBarVerticalPad + maxOf(
            LabDimens.HomeProfileSize,
            barTopInset + panelHeight,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState),
            verticalArrangement = Arrangement.spacedBy(LabDimens.TabSectionGap),
        ) {
            Spacer(modifier = Modifier.height(headerBlockHeight))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(chromeAlpha),
            ) {
                if (expandProgress < 0.92f) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(LabDimens.TabSectionGap),
                    ) {
                        if (loadedChannels.isNotEmpty()) {
                            ChannelStoriesRow(
                                channels = loadedChannels,
                                onSelect = { chatId, bounds ->
                                    val ids = loadedChannels.map { it.chatId }
                                    if (storiesViewModel != null) {
                                        storySession = StorySession(chatId, ids, bounds)
                                    } else {
                                        onChannelClick(chatId)
                                    }
                                },
                            )
                        }
                        TabRow(active = tab, onSelect = { tab = it })
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(LabColors.Black)
                    .alpha(feedAlpha),
            ) {
                if (tab == HomeTab.Sigils) {
                    SigilsTab()
                } else {
                    val filtered = loadedItems.filter {
                        when (tab) {
                            HomeTab.Spaces -> it.category == ChatCategory.Space
                            HomeTab.Mail -> it.category == ChatCategory.Mail
                            HomeTab.Sigils -> false
                        }
                    }
                    val showArchiveRow = tab == HomeTab.Mail && archiveLink.visible
                    val showFeed = filtered.isNotEmpty() || showArchiveRow

                    if (!showFeed) {
                        val spaceCount = loadedItems.count { it.category == ChatCategory.Space }
                        val mailCount = loadedItems.count { it.category == ChatCategory.Mail }
                        val channelCount = loadedItems.count { it.category == ChatCategory.Channel }
                        val hint = when {
                            loadedItems.isEmpty() ->
                                if (tab == HomeTab.Spaces) spacesEmptyHint else mailEmptyHint
                            tab == HomeTab.Spaces && spaceCount == 0 ->
                                "No group chats in this tab. ($mailCount direct, $channelCount channel chats in your inbox.)"
                            tab == HomeTab.Mail && mailCount == 0 ->
                                "No 1:1 chats in this tab. ($spaceCount groups, $channelCount channel chats in your inbox.)"
                            else -> if (tab == HomeTab.Spaces) spacesEmptyHint else mailEmptyHint
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = hint,
                                color = LabColors.White33,
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
                                start = LabDimens.HomeBarPadding,
                                end = LabDimens.HomeBarPadding,
                                bottom = AppInsets.navigationBarBottom() + LabDimens.TabContentBottomPad,
                            ),
                            verticalArrangement = Arrangement.spacedBy(LabDimens.TabSectionGap),
                        ) {
                            if (showArchiveRow) {
                                item(key = "archive-link") {
                                    ArchiveRow(
                                        unreadCount = archiveLink.unreadCount,
                                        onClick = onArchiveClick,
                                    )
                                }
                            }
                            items(filtered, key = { it.chatId }) { item ->
                                ChatInboxCard(item = item, onClick = { onChatClick(item.chatId) })
                            }
                        }
                    }
                }
            }
        }

        if (searchExpanded) {
            PolliScreenScrim(
                onDismiss = { closeSearchPanel() },
                hazeState = hazeState,
            )
        }

        HomeExpandableSearchHeader(
            expandProgress = expandProgress,
            panelHeight = panelHeight,
            onPanelBodyHeightChanged = { searchPanelBodyHeight = it },
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
        )

        storySession?.let { session ->
            storiesViewModel?.let { vm ->
                ChannelStoriesOverlay(
                    session = session,
                    storiesViewModel = vm,
                    onClose = { storySession = null },
                )
            }
        }
    }
}

@Composable
private fun HomeExpandableSearchHeader(
    expandProgress: Float,
    panelHeight: androidx.compose.ui.unit.Dp,
    onPanelBodyHeightChanged: (androidx.compose.ui.unit.Dp) -> Unit,
    profileName: String,
    query: String,
    searchPanelOpen: Boolean,
    hazeState: dev.chrisbanes.haze.HazeState,
    onProfileClick: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onPlusClick: () -> Unit,
    focusRequester: FocusRequester,
) {
    val cornerRadius = lerp(LabDimens.HomeBarHeight / 2, 20.dp, expandProgress)
    val profileSlotWidth = lerp(
        LabDimens.HomeProfileSize + LabDimens.HomeProfileGap,
        0.dp,
        expandProgress,
    )
    val profileAlpha = (1f - expandProgress).coerceIn(0f, 1f)
    val barTopInset = (LabDimens.HomeProfileSize - LabDimens.HomeBarHeight) / 2
    val showField = searchPanelOpen || expandProgress > 0.08f

    Column(
        modifier = Modifier
            .padding(horizontal = LabDimens.HomeBarPadding)
            .padding(top = LabDimens.HomeBarVerticalPad),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(profileSlotWidth)
                    .height(LabDimens.HomeProfileSize)
                    .clipToBounds(),
                contentAlignment = Alignment.CenterStart,
            ) {
                SelfAvatar(
                    name = profileName,
                    size = LabDimens.HomeProfileSize,
                    onClick = onProfileClick,
                    modifier = Modifier.alpha(profileAlpha),
                )
            }

            HomeSearchPillSurface(
                cornerRadius = cornerRadius,
                hazeState = hazeState,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = barTopInset)
                    .height(panelHeight),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(LabDimens.HomeBarHeight)
                            .then(
                                if (!showField) {
                                    Modifier.clickable(onClick = onOpenSearch)
                                } else {
                                    Modifier
                                },
                            )
                            .padding(
                                start = LabDimens.HomePillInsetBeforeSearch,
                                end = 6.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LabIcon(
                            LabIconName.Search,
                            LabDimens.HomeSearchGlyphSize,
                            LabColors.White33,
                        )
                        if (showField) {
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = LabDimens.HomeSearchGapAfterGlyph)
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.labelMedium.copy(
                                    color = LabColors.White85,
                                ),
                                cursorBrush = SolidColor(LabColors.White),
                                singleLine = true,
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (query.isEmpty()) {
                                            Text("Search", color = LabColors.White16)
                                        }
                                        inner()
                                    }
                                },
                            )
                        } else {
                            Spacer(modifier = Modifier.width(LabDimens.HomeSearchGapAfterGlyph))
                            Text(
                                "Search",
                                color = LabColors.White16,
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
                        onHeightMeasured = onPanelBodyHeightChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelStoriesRow(
    channels: List<InboxItem>,
    onSelect: (chatId: Int, bounds: StoryLaunchBounds) -> Unit,
) {
    if (channels.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LabDimens.StoriesRowPadding)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(LabDimens.StoryRingSpacing),
    ) {
        channels.forEach { channel ->
            StoryRing(
                channel = channel,
                onClick = { bounds -> onSelect(channel.chatId, bounds) },
            )
        }
    }
}

@Composable
private fun StoryRing(channel: InboxItem, onClick: (StoryLaunchBounds) -> Unit) {
    var launchBounds by remember(channel.chatId) { mutableStateOf<StoryLaunchBounds?>(null) }
    Box(
        modifier = Modifier
            .size(LabDimens.StoryRingOuter)
            .onGloballyPositioned { coords ->
                val rect = coords.boundsInRoot()
                launchBounds = StoryLaunchBounds(
                    centerX = rect.left + rect.width / 2f,
                    centerY = rect.top + rect.height / 2f,
                    size = minOf(rect.width, rect.height),
                )
            }
            .clip(CircleShape)
            .background(accent().solid)
            .padding(LabDimens.StoryRingStroke)
            .clickable {
                launchBounds?.let(onClick)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(LabColors.Black)
                .padding(LabDimens.StoryRingGap),
            contentAlignment = Alignment.Center,
        ) {
            LabAvatar(
                name = channel.name,
                seed = channel.colorSeed,
                size = LabDimens.StoryRingInner,
                chatId = channel.chatId,
            )
        }
    }
}

@Composable
private fun TabRow(active: HomeTab, onSelect: (HomeTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LabDimens.HomeBarPadding),
        horizontalArrangement = Arrangement.spacedBy(LabDimens.TabGap),
    ) {
        TabPill("Spaces", active == HomeTab.Spaces) { onSelect(HomeTab.Spaces) }
        TabPill("Mail", active == HomeTab.Mail) { onSelect(HomeTab.Mail) }
        TabPill("Sigils", active == HomeTab.Sigils) { onSelect(HomeTab.Sigils) }
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) {
        accent().gradientBrush(0.66f)
    } else {
        Brush.linearGradient(listOf(LabColors.White8, LabColors.White8))
    }
    Box(
        modifier = Modifier
            .height(LabDimens.TabButtonHeight)
            .clip(RoundedCornerShape(17.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = LabDimens.TabButtonHPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) LabColors.White else LabColors.White66,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ArchiveRow(unreadCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(LabDimens.AvatarSize)
                .clip(CircleShape)
                .background(LabColors.Gray66),
            contentAlignment = Alignment.Center,
        ) {
            Text("⌂", color = LabColors.White66, fontSize = 20.sp)
        }
        Text(
            text = stringResource(R.string.chat_archived_chats_title),
            color = LabColors.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .height(LabDimens.UnreadBadgeMinSize)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent().gradientBrush())
                    .padding(horizontal = LabDimens.UnreadBadgeHPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    color = LabColors.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun ChatInboxCard(item: InboxItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.width(LabDimens.AvatarSize).padding(top = 2.dp)) {
                LabAvatar(
                    name = item.name,
                    seed = item.colorSeed,
                    size = LabDimens.AvatarSize,
                    chatId = item.chatId,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 2.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = LabDimens.InboxTitleRowMinHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.name,
                        color = LabColors.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatRelativeTime(item.updatedAt),
                        color = LabColors.White33,
                        fontSize = 12.sp,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = LabDimens.GroupNameNotifGap)
                        .heightIn(min = LabDimens.InboxPreviewRowMinHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = buildString {
                            item.previewAuthor?.let { append("$it: ") }
                            append(if (item.preview.isBlank()) "No messages yet" else item.preview)
                        },
                        color = LabColors.White66,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(LabDimens.UnreadBadgeMinSize)
                                .clip(RoundedCornerShape(999.dp))
                                .background(accent().gradientBrush())
                                .padding(horizontal = LabDimens.UnreadBadgeHPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                                color = LabColors.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(ts: Long): String {
    if (ts <= 0) return "—"
    val now = System.currentTimeMillis() / 1000
    val diff = now - ts
    return when {
        diff < 60 -> "now"
        diff < 3600 -> "${diff / 60}m"
        diff < 86400 -> "${diff / 3600}h"
        diff < 604800 -> "${diff / 86400}d"
        else -> "1w+"
    }
}
