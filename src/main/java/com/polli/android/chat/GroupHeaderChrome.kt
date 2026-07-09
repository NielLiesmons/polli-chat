package com.polli.android.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.icons.PolliIcon
import com.polli.android.icons.PolliIconName
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens
import com.polli.android.theme.accent
import com.polli.android.ui.AppInsets
import com.polli.android.ui.FrostedChromeSurface
import com.polli.android.ui.FrostedCircleButton
import com.polli.android.ui.PolliAvatar
import com.polli.android.ui.RoundBackButton
import com.polli.android.ui.polliHazeStyle
import com.polli.core.chat.ChatDetailTab
import com.polli.core.chat.tabsForChat
import com.polli.ui.components.TabCountBadge
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeState

private val TAB_SELECTED_HEIGHT = PolliDimens.TabButtonHeight
private val TAB_UNSELECTED_HEIGHT = PolliDimens.TabButtonUnselectedHeight
private val TAB_SELECTED_FONT = 14.5.sp
private val TAB_UNSELECTED_FONT = 13.sp
private val TAB_SEARCH_FONT = 11.sp
private val TAB_SELECTED_H_PADDING = PolliDimens.TabButtonHPadding
private val TAB_UNSELECTED_H_PADDING = PolliDimens.TabButtonUnselectedHPadding
private val TAB_SELECTED_CORNER = 17.dp
private val TAB_UNSELECTED_CORNER = PolliDimens.TabButtonUnselectedCorner

private data class TabLayoutInRow(val leftPx: Float, val widthPx: Float)

@Composable
fun GroupHeaderChrome(
    chatTitle: String,
    chatId: Int,
    isGroup: Boolean,
    isBroadcast: Boolean,
    selectedTab: ChatDetailTab,
    onTabSelected: (ChatDetailTab) -> Unit,
    onBack: () -> Unit,
    onTitleClick: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
) {
    val tabs = remember(isGroup, isBroadcast) { tabsForChat(isGroup, isBroadcast) }

    Column(modifier = modifier.fillMaxWidth()) {
        ChatHeaderTitleRow(
            title = chatTitle,
            chatId = chatId,
            chatSeed = chatTitle,
            onBack = onBack,
            onTitleClick = onTitleClick,
            hazeState = hazeState,
        )
        if (tabs.size > 1) {
            ChatHeaderTabRow(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                hazeState = hazeState,
            )
        }
    }
}

@Composable
internal fun ChatHeaderTitleRow(
    title: String,
    chatId: Int,
    chatSeed: String,
    onBack: () -> Unit,
    onTitleClick: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
) {
    val sideInset = PolliDimens.DetailBackButtonSize + PolliDimens.HomeBarPadding
    val titleInteraction = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    top = AppInsets.statusBarTop() + 6.dp,
                    start = PolliDimens.HomeBarPadding,
                    end = PolliDimens.HomeBarPadding,
                    bottom = 6.dp,
                ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBackButton(
                onClick = onBack,
                hazeState = hazeState,
                iconSize = 20.dp,
                iconEndPadding = 3.dp,
            )
            ChatHeaderAvatarButton(
                name = title,
                seed = chatSeed,
                chatId = chatId,
                hazeState = hazeState,
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = sideInset)
                    .then(
                        if (onTitleClick != null) {
                            Modifier.clickable(
                                interactionSource = titleInteraction,
                                indication = null,
                                onClick = onTitleClick,
                            )
                        } else {
                            Modifier
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                color = PolliColors.White85,
                style =
                    TextStyle(
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun ChatHeaderAvatarButton(
    name: String,
    seed: String,
    chatId: Int,
    hazeState: HazeState? = null,
) {
    val buttonSize = PolliDimens.DetailBackButtonSize
    val avatarSize = buttonSize - 4.dp
    if (hazeState != null) {
        FrostedCircleButton(
            onClick = {},
            hazeState = hazeState,
            modifier = Modifier.size(buttonSize),
        ) {
            PolliAvatar(
                name = name,
                seed = seed,
                size = avatarSize,
                chatId = chatId,
            )
        }
    } else {
        Box(
            modifier =
                Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(PolliColors.Gray66),
            contentAlignment = Alignment.Center,
        ) {
            PolliAvatar(
                name = name,
                seed = seed,
                size = avatarSize,
                chatId = chatId,
            )
        }
    }
}

@Composable
private fun ChatHeaderTabRow(
    tabs: List<ChatDetailTab>,
    selectedTab: ChatDetailTab,
    onTabSelected: (ChatDetailTab) -> Unit,
    hazeState: HazeState?,
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val tabLayouts = remember { mutableStateMapOf<ChatDetailTab, TabLayoutInRow>() }
    var rowCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var didInitialTabScroll by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val edgePad = (maxWidth / 2) - TAB_SELECTED_HEIGHT

        val selectedLayout = tabLayouts[selectedTab]
        LaunchedEffect(selectedTab, selectedLayout, viewportWidthPx, didInitialTabScroll) {
            val layout = selectedLayout ?: return@LaunchedEffect
            if (viewportWidthPx <= 0f) return@LaunchedEffect
            val tabCenter = layout.leftPx + layout.widthPx / 2f
            val targetScroll = (tabCenter - viewportWidthPx / 2f).toInt().coerceAtLeast(0)
            if (!didInitialTabScroll) {
                scrollState.scrollTo(targetScroll)
                didInitialTabScroll = true
            } else {
                scrollState.animateScrollTo(targetScroll)
            }
        }

        Row(
            modifier =
                Modifier
                    .horizontalScroll(scrollState)
                    .onGloballyPositioned { rowCoords = it }
                    .padding(bottom = PolliDimens.TabSectionGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(edgePad.coerceAtLeast(48.dp)))

            tabs.forEachIndexed { index, tab ->
                if (index > 0) {
                    Spacer(Modifier.width(PolliDimens.ChatHeaderTabGap))
                }

                val tabModifier =
                    Modifier.onGloballyPositioned { coords ->
                        val row = rowCoords ?: return@onGloballyPositioned
                        val left = coords.positionInWindow().x - row.positionInWindow().x
                        tabLayouts[tab] = TabLayoutInRow(left, coords.size.width.toFloat())
                    }

                if (tab == ChatDetailTab.Search) {
                    ChatHeaderSearchPill(
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                        hazeState = hazeState,
                        modifier = tabModifier,
                    )
                } else {
                    ChatHeaderTabPill(
                        label = tab.label,
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                        hazeState = hazeState,
                        modifier = tabModifier,
                    )
                }
            }

            Spacer(Modifier.width(edgePad.coerceAtLeast(48.dp)))
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun ChatHeaderSearchPill(
    selected: Boolean,
    onClick: () -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
) {
    val height = if (selected) TAB_SELECTED_HEIGHT else TAB_UNSELECTED_HEIGHT
    val hPadding = if (selected) TAB_SELECTED_H_PADDING else TAB_UNSELECTED_H_PADDING
    val corner = if (selected) TAB_SELECTED_CORNER else TAB_UNSELECTED_CORNER
    val shape = RoundedCornerShape(corner)
    val interaction = remember { MutableInteractionSource() }

    FrostedChromeSurface(
        modifier = modifier.height(height),
        shape = shape,
        tint = PolliColors.Gray33,
        borderColor = PolliColors.ShellBorder,
        hazeState = hazeState,
        hazeStyle = polliHazeStyle(PolliColors.Gray33),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                    .padding(horizontal = hPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PolliIcon(
                PolliIconName.Search,
                PolliDimens.HomeSearchGlyphSize,
                PolliColors.White33,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Search",
                color = PolliColors.White33,
                fontSize = TAB_SEARCH_FONT,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun ChatHeaderTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    hazeState: HazeState?,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    val height = if (selected) TAB_SELECTED_HEIGHT else TAB_UNSELECTED_HEIGHT
    val hPadding = if (selected) TAB_SELECTED_H_PADDING else TAB_UNSELECTED_H_PADDING
    val corner = if (selected) TAB_SELECTED_CORNER else TAB_UNSELECTED_CORNER
    val fontSize = if (selected) TAB_SELECTED_FONT else TAB_UNSELECTED_FONT
    val shape = RoundedCornerShape(corner)
    val interaction = remember { MutableInteractionSource() }
    val borderColor = if (selected) Color.Transparent else PolliColors.ShellBorder

    FrostedChromeSurface(
        modifier = modifier.height(height),
        shape = shape,
        tint = PolliColors.Gray66,
        borderColor = borderColor,
        hazeState = hazeState,
        hazeStyle = if (selected) polliHazeStyle(accent().solid) else null,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (selected) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .background(accent().gradientBrush(0.66f)),
                )
            }
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onClick,
                        )
                        .padding(horizontal = hPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected) PolliColors.White else PolliColors.White66,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            if (badgeCount > 0) {
                TabCountBadge(
                    count = badgeCount,
                    selected = selected,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}
