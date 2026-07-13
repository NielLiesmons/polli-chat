package com.polli.android.chat

import com.polli.domain.model.chat.ChatMessage
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.polli.android.R
import com.polli.android.icons.PolliIcon
import com.polli.android.icons.PolliIconName
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens
import com.polli.android.ui.AppInsets
import com.polli.android.ui.FrostedChromeSurface
import com.polli.android.ui.PolliAvatar
import com.polli.android.ui.PolliModalBarrier
import com.polli.android.ui.polliOverlayHazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BubbleOverlayAnchor(
    val message: ChatMessage,
    val tapX: Float,
    val tapY: Float,
)

private val PanelShape = RoundedCornerShape(16.dp)
private val ActionsPanelWidth = 280.dp
private val ReactionsPanelHeight = 52.dp
private val ReactionsFadeWidth = 52.dp
private val PlusButtonSize = 36.dp
private val PanelGap = 8.dp
private val EdgePad = 16.dp
private val OverlayShellBg = PolliColors.Gray66
private val OverlayShellBorder = PolliColors.ShellBorder
private val ActionRowVPad = 10.dp
private const val ActionRowCount = 7

private fun estimateActionsPanelHeightPx(includeSeenBy: Boolean, density: Density): Float =
    with(density) {
        val row = ActionRowVPad * 2 + 24.dp
        val divider = 1.dp
        val seenByBlock = if (includeSeenBy) row + divider else 0.dp
        (seenByBlock + row * ActionRowCount + divider * (ActionRowCount - 1)).toPx()
    }

@Composable
fun BubbleOverlayHost(
    anchor: BubbleOverlayAnchor?,
    hazeState: HazeState?,
    keyboardVisible: Boolean,
    chatSession: com.polli.domain.model.chat.ChatActionContext,
    onDismiss: () -> Unit,
    onReaction: (String) -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onReport: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
) {
    if (anchor == null) return
    val context = LocalContext.current
    var showEmojiPicker by remember { mutableStateOf(false) }
    val quickEmojis = remember(anchor.message.id) { RecentEmojiStore.orderedQuickPick(context) }

    BackHandler(onBack = onDismiss)

    EmojiPickerModal(
        visible = showEmojiPicker,
        hazeState = hazeState,
        onPick = { emoji ->
            RecentEmojiStore.record(context, emoji)
            onReaction(emoji)
        },
        onDismiss = { showEmojiPicker = false },
    )

    val emojiScroll = rememberScrollState()
    val overlayHazeStyle = remember { polliOverlayHazeStyle(OverlayShellBg) }
    val panelTapBlock = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val statusTopPx = with(density) { AppInsets.statusBarTop().toPx() }
    val navBottomPx = with(density) { AppInsets.navigationBarBottom().toPx() }
    val edgePadPx = with(density) { EdgePad.toPx() }
    val actionsWidthPx = with(density) { ActionsPanelWidth.toPx() }
    val reactionsWidthPx = with(density) { ActionsPanelWidth.toPx() + with(density) { ReactionsFadeWidth.toPx() } }
    val reactionsHeightPx = with(density) { ReactionsPanelHeight.toPx() }
    val gapPx = with(density) { PanelGap.toPx() }
    val actionsHeightPx =
        remember(anchor.message.id, anchor.message.isOutgoing) {
            estimateActionsPanelHeightPx(anchor.message.isOutgoing, density)
        }
    var hostWindowOrigin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .zIndex(200f),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(PolliModalBarrier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
        )

        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        hostWindowOrigin = coords.positionInWindow()
                    },
        ) {
            val screenW = constraints.maxWidth.toFloat()
            val screenH = constraints.maxHeight.toFloat()
            val safeTop = statusTopPx + edgePadPx
            val safeBottom = screenH - navBottomPx - edgePadPx

            val tapX = anchor.tapX - hostWindowOrigin.x
            val tapY = anchor.tapY - hostWindowOrigin.y

            val reactionsCenterY =
                tapY.coerceIn(
                    safeTop + reactionsHeightPx / 2f,
                    safeBottom - reactionsHeightPx / 2f,
                )
            val reactionsTop = reactionsCenterY - reactionsHeightPx / 2f
            val reactionsLeft =
                (tapX - reactionsWidthPx / 2f)
                    .coerceIn(edgePadPx, (screenW - edgePadPx - reactionsWidthPx).coerceAtLeast(edgePadPx))

            val lowerZoneStart = if (keyboardVisible) screenH * 0.5f else screenH * (2f / 3f)
            val actionsAbove = tapY >= lowerZoneStart

            val actionsLeft =
                (tapX - actionsWidthPx / 2f)
                    .coerceIn(edgePadPx, (screenW - edgePadPx - actionsWidthPx).coerceAtLeast(edgePadPx))
            val actionsTop =
                (
                    if (actionsAbove) {
                        reactionsTop - gapPx - actionsHeightPx
                    } else {
                        reactionsTop + reactionsHeightPx + gapPx
                    }
                ).coerceIn(safeTop, (safeBottom - actionsHeightPx).coerceAtLeast(safeTop))

            FrostedChromeSurface(
                modifier =
                    Modifier
                        .offset {
                            IntOffset(reactionsLeft.roundToInt(), reactionsTop.roundToInt())
                        }
                        .width(ActionsPanelWidth + ReactionsFadeWidth)
                        .height(ReactionsPanelHeight)
                        .clickable(
                            interactionSource = panelTapBlock,
                            indication = null,
                            onClick = {},
                        ),
                shape = PanelShape,
                tint = OverlayShellBg,
                borderColor = OverlayShellBorder,
                hazeState = hazeState,
                hazeStyle = overlayHazeStyle,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .horizontalScroll(emojiScroll)
                                .padding(start = 14.dp, end = ReactionsFadeWidth + 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        quickEmojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                lineHeight = 28.sp,
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            RecentEmojiStore.record(context, emoji)
                                            onReaction(emoji)
                                        }
                                        .padding(horizontal = 2.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .width(ReactionsFadeWidth)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        0f to Color.Transparent,
                                        0.35f to OverlayShellBg.copy(alpha = 0.85f),
                                        1f to OverlayShellBg,
                                    ),
                                ),
                    )
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                                .size(PlusButtonSize)
                                .clip(CircleShape)
                                .background(OverlayShellBg)
                                .border(PolliDimens.ShellBorderWidth, OverlayShellBorder, CircleShape)
                                .clickable { showEmojiPicker = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        PolliIcon(PolliIconName.Plus, 18.dp, PolliColors.White66)
                    }
                }
            }

            ActionsPanel(
                modifier =
                    Modifier
                        .offset {
                            IntOffset(actionsLeft.roundToInt(), actionsTop.roundToInt())
                        }
                        .width(ActionsPanelWidth)
                        .clickable(
                            interactionSource = panelTapBlock,
                            indication = null,
                            onClick = {},
                        ),
                message = anchor.message,
                hazeState = hazeState,
                overlayHazeStyle = overlayHazeStyle,
                onReply = onReply,
                onForward = onForward,
                onReport = onReport,
                onDetails = onDetails,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun ActionsPanel(
    modifier: Modifier,
    message: ChatMessage,
    hazeState: HazeState?,
    overlayHazeStyle: HazeStyle,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onReport: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var readers by remember(message.id) { mutableStateOf<List<ReadReceiptUser>>(emptyList()) }
    LaunchedEffect(message.id) {
        if (message.isOutgoing) {
            readers =
                withContext(Dispatchers.IO) {
                    ReadReceipts.load(context, message.id)
                }
        }
    }

    FrostedChromeSurface(
        modifier = modifier.wrapContentHeight(),
        shape = PanelShape,
        tint = OverlayShellBg,
        borderColor = OverlayShellBorder,
        hazeState = hazeState,
        hazeStyle = overlayHazeStyle,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (message.isOutgoing && readers.isNotEmpty()) {
                SeenByRow(readers = readers)
                HorizontalDivider(color = PolliColors.White8)
            }
            OverlayActionRow(
                label = stringResource(R.string.notify_reply_button),
                iconRes = null,
                polliIcon = PolliIconName.Reply,
                onClick = onReply,
            )
            HorizontalDivider(color = PolliColors.White8)
            OverlayActionRow(
                label = stringResource(R.string.menu_forward),
                iconRes = R.drawable.ic_forward_white_24dp,
                onClick = onForward,
            )
            HorizontalDivider(color = PolliColors.White8)
            OverlayActionRow(
                label = "Pin",
                iconRes = R.drawable.baseline_bookmark_border_24,
                enabled = false,
                onClick = {},
            )
            HorizontalDivider(color = PolliColors.White8)
            OverlayActionRow(
                label = "Report",
                iconRes = null,
                polliIcon = PolliIconName.Options,
                onClick = onReport,
            )
            HorizontalDivider(color = PolliColors.White8)
            OverlayActionRow(
                label = stringResource(R.string.info),
                iconRes = R.drawable.ic_help_24dp,
                onClick = onDetails,
            )
            HorizontalDivider(color = PolliColors.White8)
            OverlayActionRow(
                label = stringResource(R.string.delete),
                iconRes = R.drawable.ic_delete_white_24dp,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun SeenByRow(readers: List<ReadReceiptUser>) {
    val avatarSize = 22.dp
    val step = 14.dp
    val stackWidth = avatarSize + step * (readers.size - 1).coerceAtLeast(0)
    val label =
        when (readers.size) {
            1 -> "Seen by ${readers.first().name}"
            else -> "Seen by ${readers.size}"
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PolliDimens.ChatBubbleInsetH, vertical = ActionRowVPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(stackWidth)) {
            readers.take(4).forEachIndexed { index, reader ->
                PolliAvatar(
                    name = reader.name,
                    seed = reader.contactId.toString(),
                    size = avatarSize,
                    contactId = reader.contactId,
                    modifier = Modifier.offset(x = step * index),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = PolliColors.White66,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun OverlayActionRow(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconRes: Int? = null,
    polliIcon: PolliIconName? = null,
) {
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (enabled) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = PolliDimens.ChatBubbleInsetH, vertical = ActionRowVPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when {
                polliIcon != null -> PolliIcon(polliIcon, 20.dp, PolliColors.White66)
                iconRes != null ->
                    androidx.compose.foundation.Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(PolliColors.White66),
                        modifier = Modifier.size(20.dp),
                    )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = PolliColors.White85.copy(alpha = alpha),
            fontSize = 15.sp,
        )
    }
}
