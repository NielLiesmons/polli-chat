package com.polli.android.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.bridge.InboxItem
import com.polli.core.chat.ChatCategory
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.ui.LabAvatar
import com.polli.android.ui.SelfAvatar
import com.polli.android.ui.AppInsets
import com.polli.android.ui.scrollFadeMask
import com.polli.android.ui.rememberLazyListShowTopFadeDerived
import org.thoughtcrime.securesms.R

enum class HomeTab { Spaces, Mail, Sigils }

@Composable
fun HomeScreen(
    profileName: String,
    profileSeed: String,
    items: List<InboxItem>? = null,
    channels: List<InboxItem>? = null,
    spacesEmptyHint: String = "No spaces yet. Your group chats appear here.",
    mailEmptyHint: String = "No mail chats yet.",
    onProfileClick: () -> Unit,
    onPlusClick: () -> Unit,
    onChatClick: (Int) -> Unit,
    onChannelClick: (Int) -> Unit,
    onSearch: (String) -> Unit,
    onArchiveClick: () -> Unit = {},
) {
    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(HomeTab.Spaces) }
    val listState = rememberLazyListState()
    val showTopFade by rememberLazyListShowTopFadeDerived(listState)

    val loadedItems = items ?: rememberInboxItems(query)
    val loadedChannels = channels ?: rememberChannels(loadedItems)
    val archiveLink = rememberArchiveLinkState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.Black)
            .padding(top = AppInsets.statusBarTop()),
        verticalArrangement = Arrangement.spacedBy(LabDimens.TabSectionGap),
    ) {
        HomeTopBar(
            profileName = profileName,
            profileSeed = profileSeed,
            isSearching = isSearching,
            query = query,
            onProfileClick = onProfileClick,
            onActivateSearch = { isSearching = true },
            onCancelSearch = { isSearching = false; query = ""; onSearch("") },
            onQueryChange = { query = it; onSearch(it) },
            onPlusClick = onPlusClick,
        )

        if (!isSearching) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LabDimens.TabSectionGap),
            ) {
                if (loadedChannels.isNotEmpty()) {
                    ChannelStoriesRow(channels = loadedChannels, onSelect = onChannelClick)
                }
                TabRow(active = tab, onSelect = { tab = it })
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(LabColors.Black),
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
}

@Composable
private fun HomeTopBar(
    profileName: String,
    profileSeed: String,
    isSearching: Boolean,
    query: String,
    onProfileClick: () -> Unit,
    onActivateSearch: () -> Unit,
    onCancelSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onPlusClick: () -> Unit,
) {
    val profileAlpha by animateFloatAsState(if (isSearching) 0f else 1f, label = "profileAlpha")
    Row(
        modifier = Modifier
            .padding(horizontal = LabDimens.HomeBarPadding)
            .padding(vertical = LabDimens.HomeBarVerticalPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
            if (!isSearching) {
                SelfAvatar(
                    name = profileName,
                    size = LabDimens.HomeProfileSize,
                    onClick = onProfileClick,
                    modifier = Modifier.alpha(profileAlpha),
                )
                Spacer(modifier = Modifier.width(LabDimens.HomeProfileGap))
            }
            if (isSearching) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(LabDimens.HomeBarHeight)
                        .clip(RoundedCornerShape(LabDimens.HomeBarHeight / 2))
                        .background(LabColors.Gray33)
                        .padding(horizontal = 14.dp),
                    textStyle = MaterialTheme.typography.labelMedium.copy(color = LabColors.White85),
                    cursorBrush = SolidColor(LabColors.Blurple),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text("Search", color = LabColors.White33)
                            inner()
                        }
                    },
                )
                IconButton(onClick = onCancelSearch, modifier = Modifier.size(LabDimens.HomePillActionSize)) {
                    LabIcon(LabIconName.Cross, 12.dp, LabColors.White33)
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(LabDimens.HomeBarHeight)
                        .clip(RoundedCornerShape(LabDimens.HomeBarHeight / 2))
                        .background(
                            Brush.horizontalGradient(listOf(LabColors.Gray33, Color.Transparent)),
                        )
                        .clickable(onClick = onActivateSearch)
                        .padding(start = LabDimens.HomePillInsetBeforeSearch),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LabIcon(LabIconName.Search, LabDimens.HomeSearchGlyphSize, LabColors.White33)
                    Spacer(modifier = Modifier.width(LabDimens.HomeSearchGapAfterGlyph))
                    Text("Search", color = LabColors.White33, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(LabDimens.HomePillActionSize)
                        .clip(CircleShape)
                        .background(LabColors.Gray66)
                        .clickable(onClick = onPlusClick),
                    contentAlignment = Alignment.Center,
                ) {
                    LabIcon(LabIconName.Plus, 14.dp, LabColors.White66)
                }
            }
    }
}

@Composable
private fun ChannelStoriesRow(channels: List<InboxItem>, onSelect: (Int) -> Unit) {
    if (channels.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LabDimens.StoriesRowPadding)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(LabDimens.StoryRingSpacing),
    ) {
        channels.forEach { channel ->
            StoryRing(channel = channel, onClick = { onSelect(channel.chatId) })
        }
    }
}

@Composable
private fun StoryRing(channel: InboxItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(LabDimens.StoryRingOuter)
            .clip(CircleShape)
            .background(LabColors.Blurple)
            .padding(LabDimens.StoryRingStroke)
            .clickable(onClick = onClick),
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
        Brush.linearGradient(listOf(LabColors.BlurpleGradientStart.copy(0.66f), LabColors.BlurpleGradientEnd.copy(0.66f)))
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
            .clickable(onClick = onClick)
            .padding(vertical = LabDimens.ListRowPadding),
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
                    .background(Brush.linearGradient(listOf(LabColors.BlurpleGradientStart, LabColors.BlurpleGradientEnd)))
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
            .clickable(onClick = onClick)
            .padding(vertical = LabDimens.ListRowPadding),
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
                                .background(Brush.linearGradient(listOf(LabColors.BlurpleGradientStart, LabColors.BlurpleGradientEnd)))
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
