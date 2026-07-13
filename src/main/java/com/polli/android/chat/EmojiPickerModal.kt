package com.polli.android.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.emoji2.emojipicker.EmojiPickerView
import com.polli.android.theme.PolliColors
import com.polli.android.ui.FrostedChromeSurface
import com.polli.android.ui.PolliModalBarrier
import com.polli.android.ui.polliOverlayHazeStyle
import dev.chrisbanes.haze.HazeState

/** Full-screen overlay (not Dialog) — sits above [BubbleOverlayHost]. */
@Composable
fun EmojiPickerModal(
    visible: Boolean,
    hazeState: HazeState?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val context = LocalContext.current
    val recent = remember(visible) { RecentEmojiStore.load(context) }
    val overlayHazeStyle = remember { polliOverlayHazeStyle(PolliColors.Gray66) }
    val panelShape = RoundedCornerShape(20.dp)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .zIndex(300f)
                .background(PolliModalBarrier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        FrostedChromeSurface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            shape = panelShape,
            tint = PolliColors.Gray66,
            borderColor = PolliColors.ShellBorder,
            hazeState = hazeState,
            hazeStyle = overlayHazeStyle,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (recent.isNotEmpty()) {
                    Text(
                        text = "Recent",
                        color = PolliColors.White33,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp),
                    )
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        recent.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onPick(emoji)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                AndroidView(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .padding(bottom = 8.dp),
                    factory = { ctx ->
                        EmojiPickerView(ctx).apply {
                            setOnEmojiPickedListener { emoji ->
                                RecentEmojiStore.record(ctx, emoji.emoji)
                                onPick(emoji.emoji)
                                onDismiss()
                            }
                        }
                    },
                )
            }
        }
    }
}
