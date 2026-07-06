package com.polli.android.stories

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.polli.android.theme.PolliColors
import kotlin.math.min

private val StoryOpenEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private const val STORY_OPEN_MS = 360
private const val STORY_DISMISS_DRAG_PX = 140f

@Composable
fun ChannelStoriesOverlay(
    session: StorySession,
    storiesViewModel: StoriesViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var dragY by remember { mutableFloatStateOf(0f) }
    var dismissing by remember { mutableStateOf(false) }
    var openStarted by remember { mutableStateOf(false) }

    val skipOpenAnim = session.launchBounds.size <= 1f

    LaunchedEffect(session.channelId) {
        dragY = 0f
        dismissing = false
        if (skipOpenAnim) {
            openStarted = true
        } else {
            openStarted = false
            delay(16)
            openStarted = true
        }
    }

    val openProgress by animateFloatAsState(
        targetValue = if (openStarted) 1f else 0f,
        animationSpec = tween(STORY_OPEN_MS, easing = StoryOpenEasing),
        label = "storyOpen",
    )

    val dismissProgress by animateFloatAsState(
        targetValue = if (dismissing) 1f else 0f,
        animationSpec = tween(220, easing = StoryOpenEasing),
        label = "storyDismiss",
    )

    LaunchedEffect(dismissing, dismissProgress) {
        if (dismissing && dismissProgress >= 0.99f) {
            onClose()
        }
    }

    BackHandler(onBack = {
        if (!dismissing) dismissing = true
    })

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .zIndex(500f),
    ) {
        val screenWpx = with(density) { maxWidth.toPx() }
        val screenHpx = with(density) { maxHeight.toPx() }
        val bounds = session.launchBounds
        val startScale = (bounds.size / min(screenWpx, screenHpx)).coerceIn(0.08f, 1f)
        val startTx = bounds.centerX - screenWpx / 2f
        val startTy = bounds.centerY - screenHpx / 2f
        val dragFraction = (dragY / screenHpx).coerceIn(0f, 1f)
        val motion = openProgress * (1f - dismissProgress)
        val scale = (startScale + (1f - startScale) * motion) * (1f - dragFraction * 0.12f)
        val tx = startTx * (1f - motion)
        val ty = startTy * (1f - motion) + dragY
        val scrimAlpha = (0.35f * motion * (1f - dragFraction * 0.6f)).coerceIn(0f, 1f)
        val cornerPx = with(density) {
            val start = bounds.size / 2f
            val end = 0f
            (start + (end - start) * motion).coerceAtLeast(0f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PolliColors.Black.copy(alpha = scrimAlpha)),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = tx
                    translationY = ty
                }
                .clip(RoundedCornerShape(with(density) { cornerPx.toDp() }))
                .pointerInput(session.channelId) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragY > STORY_DISMISS_DRAG_PX) {
                                dismissing = true
                            } else {
                                dragY = 0f
                            }
                        },
                        onVerticalDrag = { _, delta ->
                            if (!dismissing) {
                                dragY = (dragY + delta).coerceAtLeast(0f)
                            }
                        },
                    )
                },
        ) {
            ChannelStoriesScreen(
                channelIds = session.channelIds,
                startIndex = session.channelIds.indexOf(session.channelId).coerceAtLeast(0),
                storiesViewModel = storiesViewModel,
                onClose = { if (!dismissing) dismissing = true },
            )
        }
    }
}
