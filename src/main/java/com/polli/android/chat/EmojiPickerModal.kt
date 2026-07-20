package com.polli.android.chat

import android.view.ViewGroup
import android.view.ContextThemeWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.emoji2.emojipicker.EmojiPickerView
import com.polli.android.R
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens
import com.polli.android.ui.AppInsets
import com.polli.android.ui.FrostedChromeSurface
import com.polli.android.ui.PolliScreenScrim
import com.polli.android.ui.polliModalSheetHazeStyle
import dev.chrisbanes.haze.HazeState

private const val ModalEnterDurationMs = 280

/** Bottom sheet emoji picker — category tabs sync with scroll (EmojiPickerView). */
@Composable
fun EmojiPickerModal(
    visible: Boolean,
    hazeState: HazeState?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val context = LocalContext.current
    val config = LocalConfiguration.current
    val bottomSafe = AppInsets.navigationBarBottom()
    val sheetHeight = (config.screenHeightDp.dp * 0.55f).coerceAtLeast(360.dp)
    val shape = RoundedCornerShape(PolliDimens.ModalRadius)
    val modalHazeStyle = remember { polliModalSheetHazeStyle() }
    val recentProvider = remember(context) { PolliRecentEmojiProvider(context.applicationContext) }

    var sheetHeightPx by remember { mutableIntStateOf(0) }
    var enter by remember { mutableStateOf(false) }
    LaunchedEffect(sheetHeightPx) {
        if (sheetHeightPx > 0) enter = true
    }

    val enterProgress by animateFloatAsState(
        targetValue = if (enter) 1f else 0f,
        animationSpec = tween(ModalEnterDurationMs, easing = FastOutSlowInEasing),
        label = "emojiPickerEnter",
    )
    val slidePx = sheetHeightPx.toFloat().coerceAtLeast(1f)
    val sheetAlpha = (enterProgress * 3.5f).coerceIn(0f, 1f)

    BackHandler(onBack = onDismiss)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .zIndex(300f),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = enterProgress },
        ) {
            PolliScreenScrim(onDismiss = onDismiss)
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PolliDimens.ModalScreenInset)
                        .padding(bottom = bottomSafe)
                        .onSizeChanged { sheetHeightPx = it.height }
                        .graphicsLayer {
                            alpha = if (sheetHeightPx == 0) 0f else sheetAlpha
                            translationY = (1f - enterProgress) * slidePx
                        },
            ) {
                FrostedChromeSurface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(sheetHeight.coerceAtMost(maxHeight))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {},
                            ),
                    shape = shape,
                    tint = PolliColors.Gray66,
                    borderColor = PolliColors.White8,
                    hazeState = hazeState,
                    hazeStyle = modalHazeStyle,
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val themed = ContextThemeWrapper(ctx, R.style.PolliEmojiPickerTheme)
                            EmojiPickerView(themed).apply {
                                layoutParams =
                                    ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    )
                                emojiGridColumns = 8
                                emojiGridRows = 6.5f
                                setRecentEmojiProvider(recentProvider)
                            }
                        },
                        update = { picker ->
                            picker.setRecentEmojiProvider(recentProvider)
                            picker.setOnEmojiPickedListener { item ->
                                onPick(item.emoji)
                                onDismiss()
                            }
                        },
                    )
                }
            }
        }
    }
}
