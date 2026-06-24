package com.polli.android.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.ui.AppInsets
import com.polli.android.ui.FrostedChromeSurface
import com.polli.android.ui.polliOverlayHazeStyle
import dev.chrisbanes.haze.HazeState

data class BubbleOverlayAnchor(
    val message: ChatMessage,
)

private val PanelShape = RoundedCornerShape(16.dp)
private val PanelWidth = 280.dp
private val PanelGap = 8.dp
private val ReactionsPanelHeight = 52.dp
private val OverlayShellBg = LabColors.Gray66
private val OverlayShellBorder = LabColors.ShellBorder

/**
 * Center-screen overlay: two frosted panels (composer chrome pattern) in a wrap-content column.
 */
@Composable
fun BubbleOverlayHost(
    anchor: BubbleOverlayAnchor?,
    hazeState: HazeState?,
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
    val statusTop = AppInsets.statusBarTop()
    val navBottom = AppInsets.navigationBarBottom()

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

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .wrapContentHeight()
                .widthIn(max = PanelWidth)
                .padding(horizontal = 24.dp)
                .padding(top = statusTop + 8.dp, bottom = navBottom + 8.dp)
                .clickable(
                    interactionSource = panelTapBlock,
                    indication = null,
                    onClick = {},
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FrostedChromeSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ReactionsPanelHeight),
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

            Spacer(modifier = Modifier.height(PanelGap))

            FrostedChromeSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
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
