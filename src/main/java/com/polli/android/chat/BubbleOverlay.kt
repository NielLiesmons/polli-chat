package com.polli.android.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.ui.AppInsets
import com.polli.android.ui.FrostedChromeSurface
import com.polli.android.ui.polliOverlayHazeStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import kotlin.math.roundToInt

data class BubbleOverlayAnchor(
    val message: ChatMessage,
    val tapX: Float,
    val tapY: Float,
)

private val PanelShape = RoundedCornerShape(16.dp)
private val ActionsPanelWidth = 280.dp
private val ReactionsPanelWidth = ActionsPanelWidth + 80.dp
private val PanelGap = 8.dp
private val ReactionsPanelHeight = 52.dp
private val EdgePad = 16.dp
private val OverlayShellBg = LabColors.Gray66
private val OverlayShellBorder = LabColors.ShellBorder

/**
 * Reactions row is anchored to the tap point. Actions sit above or below it — never shifting reactions.
 */
@Composable
fun BubbleOverlayHost(
    anchor: BubbleOverlayAnchor?,
    hazeState: HazeState?,
    keyboardVisible: Boolean,
    onDismiss: () -> Unit,
    onReaction: (String) -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
) {
    if (anchor == null) return

    BackHandler(onBack = onDismiss)

    val emojiScroll = rememberScrollState()
    val overlayHazeStyle = remember { polliOverlayHazeStyle(OverlayShellBg) }
    val panelTapBlock = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val statusTopPx = with(density) { AppInsets.statusBarTop().toPx() }
    val navBottomPx = with(density) { AppInsets.navigationBarBottom().toPx() }
    val edgePadPx = with(density) { EdgePad.toPx() }
    val actionsWidthPx = with(density) { ActionsPanelWidth.toPx() }
    val reactionsWidthPx = with(density) { ReactionsPanelWidth.toPx() }
    val reactionsHeightPx = with(density) { ReactionsPanelHeight.toPx() }
    val gapPx = with(density) { PanelGap.toPx() }
    var actionsHeightPx by remember { mutableFloatStateOf(with(density) { 148.dp.toPx() }) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(200f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenW = constraints.maxWidth.toFloat()
            val screenH = constraints.maxHeight.toFloat()
            val safeTop = statusTopPx + edgePadPx
            val safeBottom = screenH - navBottomPx - edgePadPx

            val reactionsCenterY = anchor.tapY.coerceIn(
                safeTop + reactionsHeightPx / 2f,
                safeBottom - reactionsHeightPx / 2f,
            )
            val reactionsTop = reactionsCenterY - reactionsHeightPx / 2f
            val reactionsLeft = (anchor.tapX - reactionsWidthPx / 2f)
                .coerceIn(edgePadPx, (screenW - edgePadPx - reactionsWidthPx).coerceAtLeast(edgePadPx))

            val lowerZoneStart = if (keyboardVisible) screenH * 0.5f else screenH * (2f / 3f)
            val actionsAbove = anchor.tapY >= lowerZoneStart

            val actionsLeft = (anchor.tapX - actionsWidthPx / 2f)
                .coerceIn(edgePadPx, (screenW - edgePadPx - actionsWidthPx).coerceAtLeast(edgePadPx))
            val actionsTop = (
                if (actionsAbove) {
                    reactionsTop - gapPx - actionsHeightPx
                } else {
                    reactionsTop + reactionsHeightPx + gapPx
                }
                ).coerceIn(safeTop, (safeBottom - actionsHeightPx).coerceAtLeast(safeTop))

            FrostedChromeSurface(
                modifier = Modifier
                    .offset {
                        IntOffset(reactionsLeft.roundToInt(), reactionsTop.roundToInt())
                    }
                    .width(ReactionsPanelWidth)
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
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(emojiScroll)
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MessageReactions.DEFAULT_EMOJI.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            lineHeight = 28.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onReaction(emoji) }
                                .padding(horizontal = 2.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            ActionsPanel(
                modifier = Modifier
                    .offset {
                        IntOffset(actionsLeft.roundToInt(), actionsTop.roundToInt())
                    }
                    .width(ActionsPanelWidth)
                    .clickable(
                        interactionSource = panelTapBlock,
                        indication = null,
                        onClick = {},
                    ),
                hazeState = hazeState,
                overlayHazeStyle = overlayHazeStyle,
                onReply = onReply,
                onDelete = onDelete,
                onDismiss = onDismiss,
                onHeightMeasured = { actionsHeightPx = it },
            )
        }
    }
}

@Composable
private fun ActionsPanel(
    modifier: Modifier,
    hazeState: HazeState?,
    overlayHazeStyle: HazeStyle,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onHeightMeasured: (Float) -> Unit,
) {
    FrostedChromeSurface(
        modifier = modifier
            .wrapContentHeight()
            .onGloballyPositioned { coords ->
                if (coords.size.height > 0) onHeightMeasured(coords.size.height.toFloat())
            },
        shape = PanelShape,
        tint = OverlayShellBg,
        borderColor = OverlayShellBorder,
        hazeState = hazeState,
        hazeStyle = overlayHazeStyle,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            OverlayTextRow(label = "Reply", onClick = onReply)
            OverlayTextRow(label = "Delete", destructive = true, onClick = onDelete)
            OverlayTextRow(label = "Close", onClick = onDismiss)
        }
    }
}

@Composable
private fun OverlayTextRow(
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    Text(
        text = label,
        color = if (destructive) LabColors.Destructive else LabColors.White85,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = LabDimens.ChatBubbleInsetH, vertical = 12.dp),
    )
}
