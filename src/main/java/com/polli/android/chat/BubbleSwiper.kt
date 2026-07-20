package com.polli.android.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.polli.android.icons.PolliIcon
import com.polli.android.icons.PolliIconName
import com.polli.android.theme.PolliColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MAX_DRAG = 100f
private const val TRIGGER_AT = 80f
private const val ICON_PARALLAX = 0.28f
private const val ICON_MAX_OUTWARD = 21f
private const val ICON_SIZE = 28f
private const val ICON_GLYPH = 13f
private const val DRAG_START_PX = 6f
private const val TAP_SLOP_PX = 8f
private const val CALLBACK_DELAY_MS = 100L
private const val POP_SETTLE_MS = 180L
private const val POP_SCALE = 1.22f
private val SWIPE_EASING = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val POP_EASING = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** Swipe right → reply. Tap → actions overlay. No swipe-left. */
@Composable
fun BubbleSwiper(
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    replyIconInset: Float = if (alignEnd) 4f else 8f,
    onSwipeReply: () -> Unit,
    onTap: (Offset) -> Unit,
    content: @Composable () -> Unit,
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var popping by remember { mutableStateOf(false) }
    var triggered by remember { mutableStateOf(false) }
    var contentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val scope = rememberCoroutineScope()

    fun currentTapInRoot(local: Offset): Offset {
        val coords = contentCoords
        if (coords == null || !coords.isAttached) return Offset.Zero
        return coords.localToWindow(local)
    }

    val settleTarget = if (dragging || popping) dragX else 0f
    val animatedX by animateFloatAsState(
        targetValue = settleTarget,
        animationSpec = tween(
            durationMillis = if (popping) 140 else 220,
            easing = if (popping) POP_EASING else SWIPE_EASING,
        ),
        label = "bubbleDrag",
    )

    val dragOffset = animatedX.coerceIn(0f, MAX_DRAG)
    val replyProg = (dragOffset / TRIGGER_AT).coerceIn(0f, 1f)
    val replyScale = (0.5f + 0.5f * replyProg) * if (popping) POP_SCALE else 1f
    val replyParallax = (-dragOffset * ICON_PARALLAX).coerceIn(-ICON_MAX_OUTWARD, 0f)
    val replyOpacity = if (popping) 1f else replyProg

    fun settle() {
        dragging = false
        popping = false
        triggered = false
        dragX = 0f
    }

    fun fireReply() {
        if (triggered) return
        triggered = true
        popping = true
        scope.launch {
            delay(CALLBACK_DELAY_MS)
            onSwipeReply()
            delay(POP_SETTLE_MS)
            settle()
        }
    }

    Box(
        modifier = modifier.wrapContentWidth(),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        if (replyOpacity > 0.01f) {
            SwipeRevealIcon(
                icon = PolliIconName.Reply,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = replyIconInset.dp + replyParallax.dp),
                scale = replyScale,
                alpha = replyOpacity,
                popping = popping,
            )
        }

        Box(
            modifier = Modifier
                .wrapContentWidth()
                .offset { IntOffset(dragOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        val start = down.position
                        val downUptime = down.uptimeMillis
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                        var horizontalDrag = false
                        var childHandled = down.isConsumed
                        var longPress = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (event.changes.any { it.isConsumed }) {
                                childHandled = true
                            }

                            if (!change.pressed) {
                                if (!triggered && !popping) {
                                    val dx = change.position.x - start.x
                                    val dy = change.position.y - start.y
                                    val pressDuration = change.uptimeMillis - downUptime
                                    if (
                                        !horizontalDrag &&
                                        !longPress &&
                                        !childHandled &&
                                        !change.isConsumed &&
                                        pressDuration < longPressTimeout &&
                                        abs(dx) < TAP_SLOP_PX &&
                                        abs(dy) < TAP_SLOP_PX
                                    ) {
                                        scope.launch { onTap(currentTapInRoot(change.position)) }
                                    }
                                    scope.launch { settle() }
                                }
                                break
                            }

                            if (
                                !horizontalDrag &&
                                change.uptimeMillis - downUptime >= longPressTimeout
                            ) {
                                longPress = true
                            }

                            val dx = change.position.x - start.x
                            val dy = change.position.y - start.y

                            if (!horizontalDrag) {
                                if (abs(dx) < DRAG_START_PX && abs(dy) < DRAG_START_PX) continue
                                if (abs(dx) <= abs(dy) || dx < 0f) break
                                horizontalDrag = true
                                dragging = true
                                triggered = false
                                popping = false
                            }

                            change.consume()
                            dragX = dx.coerceIn(0f, MAX_DRAG)
                            if (dragX >= TRIGGER_AT) {
                                fireReply()
                            }
                        }
                    }
                },
        ) {
            Box(
                modifier = Modifier.onGloballyPositioned { contentCoords = it },
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SwipeRevealIcon(
    icon: PolliIconName,
    modifier: Modifier,
    scale: Float,
    alpha: Float,
    popping: Boolean,
) {
    val popSpring = remember { Animatable(1f) }
    LaunchedEffect(popping) {
        if (popping) {
            popSpring.snapTo(0.55f)
            popSpring.animateTo(
                targetValue = POP_SCALE,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            popSpring.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }

    Box(
        modifier = modifier
            .size(ICON_SIZE.dp)
            .graphicsLayer {
                val s = scale * popSpring.value
                scaleX = s
                scaleY = s
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(PolliColors.Gray33),
        contentAlignment = Alignment.Center,
    ) {
        PolliIcon(icon, ICON_GLYPH.dp, PolliColors.White33)
    }
}
