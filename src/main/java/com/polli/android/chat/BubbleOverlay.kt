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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import dev.chrisbanes.haze.HazeStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BubbleOverlayAnchor(
    val message: ChatMessage,
    /** Tap position in [ChatScreen] root coordinates (see [BubbleSwiper]). */
    val tapX: Float,
    val tapY: Float,
)

private val PanelShape = RoundedCornerShape(14.dp)
private val ReactionsPanelShape = RoundedCornerShape(percent = 50)
private val ActionsPanelWidth = 168.dp
private val ReactionsPanelWidth = 220.dp
private val ReactionsPanelHeight = 44.dp
/** Narrow fade at the emoji → plus column seam only. */
private val ReactionsFadeWidth = 12.dp
private val PlusColumnWidth = 40.dp
/** Extra clearance between actions panel and reactions row when stacked. */
private val StackGap = 16.dp
private val EdgePad = 16.dp
private val OverlayShellBg = PolliColors.Gray66
private val OverlayShellBorder = PolliColors.ShellBorder
private val ActionRowVPad = 6.dp
private val ActionIconSize = 16.dp
private val ActionFontSize = 13.5.sp
private val SeenByRowHeight = 36.dp
private const val ActionRowCount = 6
private val EmojiFontSize = 24.sp
private val EmojiGap = 12.dp

@Composable
fun BubbleOverlayHost(
    anchor: BubbleOverlayAnchor?,
    keyboardVisible: Boolean,
    chatSession: com.polli.domain.model.chat.ChatActionContext,
    onDismiss: () -> Unit,
    onReaction: (String) -> Unit,
    onOpenEmojiPicker: () -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onReport: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
) {
    if (anchor == null) return
    @Suppress("UNUSED_PARAMETER")
    val unusedSession = chatSession
    val context = LocalContext.current
    val quickEmojis = remember(anchor.message.id) { RecentEmojiStore.orderedQuickPick(context) }

    BackHandler(onBack = onDismiss)

    val emojiScroll = rememberScrollState()
    val overlayHazeStyle = remember { polliOverlayHazeStyle(OverlayShellBg) }
    val reactionsTapBlock = remember { MutableInteractionSource() }
    val actionsTapBlock = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val statusTopPx = with(density) { AppInsets.statusBarTop().toPx() }
    val navBottomPx = with(density) { AppInsets.navigationBarBottom().toPx() }
    val edgePadPx = with(density) { EdgePad.toPx() }
    val actionsWidthPx = with(density) { ActionsPanelWidth.toPx() }
    val plusColumnPx = with(density) { PlusColumnWidth.toPx() }
    val emojiAreaWidthPx = with(density) { ReactionsPanelWidth.toPx() }
    val totalReactionsBarWidthPx = emojiAreaWidthPx + plusColumnPx
    val reactionsHeightPx = with(density) { ReactionsPanelHeight.toPx() }
    val stackGapPx = with(density) { StackGap.toPx() }
    val seenByHeightPx = with(density) { SeenByRowHeight.toPx() }
    val actionRowPx = with(density) { 28.dp.toPx() }
    val dividerPx = with(density) { 1.dp.toPx() }
    // Generous estimate so placement never overlaps reactions when actions sit above.
    val actionsHeightEstimatePx =
        remember(anchor.message.isOutgoing, seenByHeightPx, actionRowPx, dividerPx) {
            val seenByBlock = if (anchor.message.isOutgoing) seenByHeightPx + dividerPx else 0f
            seenByBlock + actionRowPx * ActionRowCount + dividerPx * (ActionRowCount - 1) + 8f
        }

    var readers by remember(anchor.message.id) { mutableStateOf<List<ReadReceiptUser>>(emptyList()) }
    LaunchedEffect(anchor.message.id) {
        readers =
            if (anchor.message.isOutgoing) {
                withContext(Dispatchers.IO) {
                    runCatching { ReadReceipts.load(context, anchor.message.id) }.getOrDefault(emptyList())
                }
            } else {
                emptyList()
            }
    }

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

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val screenH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            val safeTop = statusTopPx + edgePadPx
            val safeBottom = (screenH - navBottomPx - edgePadPx).coerceAtLeast(safeTop + 1f)

            fun clamp(v: Float, min: Float, max: Float): Float =
                if (!v.isFinite()) (min + max) / 2f
                else if (min >= max) (min + max) / 2f
                else v.coerceIn(min, max)

            val tapX = if (anchor.tapX.isFinite()) anchor.tapX else screenW / 2f
            val tapY = if (anchor.tapY.isFinite()) anchor.tapY else screenH / 2f

            val reactionsCenterY =
                clamp(
                    tapY,
                    safeTop + reactionsHeightPx / 2f,
                    safeBottom - reactionsHeightPx / 2f,
                )
            var reactionsTop = reactionsCenterY - reactionsHeightPx / 2f
            val reactionsLeft =
                clamp(
                    tapX - totalReactionsBarWidthPx / 2f,
                    edgePadPx,
                    (screenW - edgePadPx - totalReactionsBarWidthPx).coerceAtLeast(edgePadPx),
                )

            val lowerZoneStart = if (keyboardVisible) screenH * 0.5f else screenH * (2f / 3f)
            val actionsAbove = tapY >= lowerZoneStart

            val actionsLeft =
                clamp(
                    tapX - actionsWidthPx / 2f,
                    edgePadPx,
                    (screenW - edgePadPx - actionsWidthPx).coerceAtLeast(edgePadPx),
                )

            var actionsTop =
                if (actionsAbove) {
                    reactionsTop - stackGapPx - actionsHeightEstimatePx
                } else {
                    reactionsTop + reactionsHeightPx + stackGapPx
                }

            if (actionsAbove) {
                // Never let the actions block sit on top of the reactions row.
                val maxActionsBottom = reactionsTop - stackGapPx
                if (actionsTop + actionsHeightEstimatePx > maxActionsBottom) {
                    actionsTop = maxActionsBottom - actionsHeightEstimatePx
                }
                // Lift the whole stack if actions would clip the top safe area.
                if (actionsTop < safeTop) {
                    val lift = safeTop - actionsTop
                    actionsTop = safeTop
                    reactionsTop =
                        clamp(
                            reactionsTop + lift,
                            safeTop,
                            safeBottom - reactionsHeightPx,
                        )
                }
            } else {
                val minActionsTop = reactionsTop + reactionsHeightPx + stackGapPx
                if (actionsTop < minActionsTop) {
                    actionsTop = minActionsTop
                }
            }

            actionsTop =
                clamp(
                    actionsTop,
                    safeTop,
                    (safeBottom - actionsHeightEstimatePx).coerceAtLeast(safeTop),
                )

            if (actionsAbove) {
                val maxActionsBottom = reactionsTop - stackGapPx
                if (actionsTop + actionsHeightEstimatePx > maxActionsBottom) {
                    actionsTop = (maxActionsBottom - actionsHeightEstimatePx).coerceAtLeast(safeTop)
                }
            } else {
                val minActionsTop = reactionsTop + reactionsHeightPx + stackGapPx
                if (actionsTop < minActionsTop) {
                    actionsTop = minActionsTop.coerceAtMost(
                        (safeBottom - actionsHeightEstimatePx).coerceAtLeast(safeTop),
                    )
                }
            }

            ReactionsPanel(
                modifier =
                    Modifier
                        .offset {
                            IntOffset(
                                reactionsLeft.roundToInt().coerceIn(0, screenW.toInt()),
                                reactionsTop.roundToInt().coerceIn(0, screenH.toInt()),
                            )
                        },
                quickEmojis = quickEmojis,
                emojiScroll = emojiScroll,
                overlayHazeStyle = overlayHazeStyle,
                reactionsTapBlock = reactionsTapBlock,
                onReaction = { emoji ->
                    RecentEmojiStore.record(context, emoji)
                    onReaction(emoji)
                },
                onOpenEmojiPicker = onOpenEmojiPicker,
            )

            ActionsPanel(
                modifier =
                    Modifier
                        .offset {
                            IntOffset(
                                actionsLeft.roundToInt().coerceIn(0, screenW.toInt()),
                                actionsTop.roundToInt().coerceIn(0, screenH.toInt()),
                            )
                        }
                        .width(ActionsPanelWidth)
                        .wrapContentHeight()
                        .clickable(
                            interactionSource = actionsTapBlock,
                            indication = null,
                            onClick = {},
                        ),
                message = anchor.message,
                readers = readers,
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
private fun ReactionsPanel(
    modifier: Modifier,
    quickEmojis: List<String>,
    emojiScroll: androidx.compose.foundation.ScrollState,
    overlayHazeStyle: HazeStyle,
    reactionsTapBlock: MutableInteractionSource,
    onReaction: (String) -> Unit,
    onOpenEmojiPicker: () -> Unit,
) {
    FrostedChromeSurface(
        modifier =
            modifier
                .width(ReactionsPanelWidth + PlusColumnWidth)
                .height(ReactionsPanelHeight)
                .clickable(
                    interactionSource = reactionsTapBlock,
                    indication = null,
                    onClick = {},
                ),
        shape = ReactionsPanelShape,
        tint = OverlayShellBg,
        borderColor = OverlayShellBorder,
        hazeState = null,
        hazeStyle = overlayHazeStyle,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .horizontalScroll(emojiScroll)
                            .padding(start = 12.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(EmojiGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    quickEmojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = EmojiFontSize,
                            lineHeight = EmojiFontSize,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onReaction(emoji) }
                                    .padding(horizontal = 1.dp, vertical = 2.dp),
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
                                    1f to OverlayShellBg,
                                ),
                            ),
                )
            }
            Box(
                modifier =
                    Modifier
                        .width(PlusColumnWidth)
                        .fillMaxHeight()
                        .background(PolliColors.White8)
                        .clickable(onClick = onOpenEmojiPicker),
                contentAlignment = Alignment.Center,
            ) {
                PolliIcon(PolliIconName.Plus, 15.dp, PolliColors.White66)
            }
        }
    }
}

@Composable
private fun ActionsPanel(
    modifier: Modifier,
    message: ChatMessage,
    readers: List<ReadReceiptUser>,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onReport: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
) {
    FrostedChromeSurface(
        modifier = modifier,
        shape = PanelShape,
        tint = OverlayShellBg,
        borderColor = OverlayShellBorder,
        hazeState = null,
        hazeStyle = polliOverlayHazeStyle(OverlayShellBg),
    ) {
        Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            if (message.isOutgoing) {
                // Reserve slot so async Seen-by load does not jump panel placement.
                if (readers.isNotEmpty()) {
                    SeenByRow(readers = readers)
                } else {
                    Box(modifier = Modifier.height(SeenByRowHeight))
                }
                HorizontalDivider(color = PolliColors.White8)
            }
            OverlayActionRow(
                label = stringResource(R.string.notify_reply_button),
                polliIcon = PolliIconName.Reply,
                onClick = onReply,
            )
            HorizontalDivider(color = PolliColors.White8)
            OverlayActionRow(
                label = "Forward",
                iconRes = R.drawable.ic_forward_white_24dp,
                onClick = onForward,
            )
            HorizontalDivider(color = PolliColors.White8)
            OverlayActionRow(
                label = "Pin",
                polliIcon = PolliIconName.Options,
                enabled = false,
                onClick = {},
            )
            HorizontalDivider(color = PolliColors.White8)
            OverlayActionRow(
                label = "Report",
                polliIcon = PolliIconName.Options,
                onClick = onReport,
            )
            HorizontalDivider(color = PolliColors.White8)
            OverlayActionRow(
                label = stringResource(R.string.info),
                polliIcon = PolliIconName.Options,
                onClick = onDetails,
            )
            HorizontalDivider(color = PolliColors.White8)
            OverlayActionRow(
                label = stringResource(R.string.delete),
                polliIcon = PolliIconName.Delete,
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
                .height(SeenByRowHeight)
                .padding(horizontal = PolliDimens.ChatBubbleInsetH),
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
    polliIcon: PolliIconName? = null,
    iconRes: Int? = null,
) {
    val alpha = if (enabled) 1f else 0.45f
    val iconColor = PolliColors.White66.copy(alpha = alpha)
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
                .padding(horizontal = 12.dp, vertical = ActionRowVPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            when {
                polliIcon != null -> PolliIcon(polliIcon, ActionIconSize, iconColor)
                iconRes != null ->
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconColor),
                        modifier = Modifier.size(ActionIconSize),
                    )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = PolliColors.White85.copy(alpha = alpha),
            fontSize = ActionFontSize,
        )
    }
}
