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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.theme.LabColors
import com.polli.android.theme.accent
import com.polli.android.theme.LabDimens
import com.polli.android.ui.AppInsets
import com.polli.android.ui.FrostedChromeSurface
import com.polli.android.ui.FrostedCircleButton
import com.polli.android.ui.LabAvatar
import com.polli.android.ui.RoundBackButton
import com.polli.core.chat.ChatDetailTab
import com.polli.core.chat.tabsForChat
import dev.chrisbanes.haze.HazeState
import kotlin.math.abs
import kotlin.math.min

/** Mirrors polli `group_header_chrome.rs` tab carousel scale curve. */
private const val HEADER_TAB_SCALE_MIN = 0.76f
private const val HEADER_TAB_EDGE_MASK_PX = 32f

private val TAB_BASE_HEIGHT = LabDimens.TabButtonHeight
private val TAB_BASE_H_PADDING = LabDimens.TabButtonHPadding
private val TAB_BASE_CORNER = 17.dp
private val TAB_BASE_FONT_SIZE = 14.5.sp

private fun Dp.scaledBy(scale: Float): Dp = (value * scale).dp

private fun TextUnit.scaledBy(scale: Float): TextUnit = (value * scale).sp

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
            hazeState = hazeState,
        )
        if (tabs.size > 1) {
            ChatHeaderTabCarousel(
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
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
) {
    val sideInset = LabDimens.DetailBackButtonSize + LabDimens.HomeBarPadding

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = AppInsets.statusBarTop() + 6.dp,
                start = LabDimens.HomeBarPadding,
                end = LabDimens.HomeBarPadding,
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
        Text(
            text = title,
            color = LabColors.White85,
            style = TextStyle(
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = sideInset),
        )
    }
}

@Composable
internal fun ChatHeaderAvatarButton(
    name: String,
    seed: String,
    chatId: Int,
    hazeState: HazeState? = null,
) {
    val buttonSize = LabDimens.DetailBackButtonSize
    val avatarSize = buttonSize - 4.dp
    if (hazeState != null) {
        FrostedCircleButton(
            onClick = {},
            hazeState = hazeState,
            modifier = Modifier.size(buttonSize),
        ) {
            LabAvatar(
                name = name,
                seed = seed,
                size = avatarSize,
                chatId = chatId,
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(LabColors.Gray66),
            contentAlignment = Alignment.Center,
        ) {
            LabAvatar(
                name = name,
                seed = seed,
                size = avatarSize,
                chatId = chatId,
            )
        }
    }
}

@Composable
private fun ChatHeaderTabCarousel(
    tabs: List<ChatDetailTab>,
    selectedTab: ChatDetailTab,
    onTabSelected: (ChatDetailTab) -> Unit,
    hazeState: HazeState?,
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    // Read on every scroll frame so scales + gaps update live.
    val scrollOffsetPx = scrollState.value.toFloat()
    val scaleRangePx = with(density) { 140.dp.toPx() }

    val tabLayouts = remember { mutableStateMapOf<ChatDetailTab, TabLayoutInRow>() }
    var rowCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val w = size.width
                if (w <= 0f) return@drawWithContent
                val maskPx = HEADER_TAB_EDGE_MASK_PX.coerceAtMost(w / 3f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to androidx.compose.ui.graphics.Color.Transparent,
                            (maskPx / w).coerceIn(0f, 1f) to androidx.compose.ui.graphics.Color.Black,
                            ((w - maskPx) / w).coerceIn(0f, 1f) to androidx.compose.ui.graphics.Color.Black,
                            1f to androidx.compose.ui.graphics.Color.Transparent,
                        ),
                        startX = 0f,
                        endX = w,
                    ),
                    size = Size(w, size.height),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val viewportCenterInContent = scrollOffsetPx + viewportWidthPx / 2f

        fun tabCenterInContent(tab: ChatDetailTab): Float? {
            val layout = tabLayouts[tab] ?: return null
            return layout.leftPx + layout.widthPx / 2f
        }

        fun scaleForTab(tab: ChatDetailTab): Float {
            val center = tabCenterInContent(tab) ?: return 1f
            val dist = abs(center - viewportCenterInContent)
            return (1f - dist / scaleRangePx).coerceAtLeast(HEADER_TAB_SCALE_MIN)
        }

        LaunchedEffect(selectedTab, viewportWidthPx) {
            repeat(3) {
                withFrameNanos {}
                val layout = tabLayouts[selectedTab] ?: return@repeat
                val tabCenter = layout.leftPx + layout.widthPx / 2f
                val targetScroll = (tabCenter - viewportWidthPx / 2f).toInt().coerceAtLeast(0)
                scrollState.animateScrollTo(targetScroll)
            }
        }

        val edgePad = (maxWidth / 2) - 52.dp

        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .onGloballyPositioned { rowCoords = it }
                .padding(bottom = LabDimens.TabSectionGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(edgePad.coerceAtLeast(48.dp)))

            tabs.forEachIndexed { index, tab ->
                if (index > 0) {
                    val prevTab = tabs[index - 1]
                    val gapScale = min(scaleForTab(prevTab), scaleForTab(tab))
                    Spacer(Modifier.width(LabDimens.ChatHeaderTabGap * gapScale))
                }

                ChatHeaderTabPill(
                    label = tab.label,
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    hazeState = hazeState,
                    scale = scaleForTab(tab),
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val row = rowCoords ?: return@onGloballyPositioned
                        val left = coords.positionInWindow().x - row.positionInWindow().x
                        tabLayouts[tab] = TabLayoutInRow(left, coords.size.width.toFloat())
                    },
                )
            }

            Spacer(Modifier.width(edgePad.coerceAtLeast(48.dp)))
        }
    }
}

@Composable
private fun ChatHeaderTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    scale: Float,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
) {
    val height = TAB_BASE_HEIGHT.scaledBy(scale)
    val hPadding = TAB_BASE_H_PADDING.scaledBy(scale)
    val corner = TAB_BASE_CORNER.scaledBy(scale)
    val fontSize = TAB_BASE_FONT_SIZE.scaledBy(scale)
    val shape = RoundedCornerShape(corner)
    val selectedGradient = accent().gradientBrush(0.66f)
    FrostedChromeSurface(
        modifier = modifier
            .height(height)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = shape,
        tint = LabColors.Gray66,
        borderColor = LabColors.ShellBorder,
        hazeState = hazeState,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (selected) Modifier.background(selectedGradient) else Modifier,
                )
                .padding(horizontal = hPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (selected) LabColors.White else LabColors.White66,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
