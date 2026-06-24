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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.theme.LabColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MAX_DRAG = 92f
private const val TRIGGER_AT = 36f
private const val ICON_PARALLAX = 0.42f
private const val ICON_MAX_OUTWARD = 24f
private const val ICON_SIZE = 28f
private const val ICON_GLYPH = 13f
private const val DRAG_START_PX = 6f
private const val TAP_SLOP_PX = 8f
private const val CALLBACK_DELAY_MS = 100L
private const val POP_SETTLE_MS = 180L
private const val POP_SCALE = 1.22f
private val SWIPE_EASING = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val POP_EASING = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/** Horizontal swipe on bubbles — port of polli/bubble_swiper.rs */
@Composable
fun BubbleSwiper(
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    replyIconInset: Float = if (alignEnd) 4f else 8f,
    optionsIconInset: Float = if (alignEnd) 4f else 8f,
    onSwipeReply: () -> Unit,
    onSwipeOptions: (Rect) -> Unit,
    onTap: ((Rect) -> Unit)? = null,
    onDragProgress: ((dragX: Float) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var popping by remember { mutableStateOf(false) }
    var popReply by remember { mutableStateOf<Boolean?>(null) }
    var triggered by remember { mutableStateOf(false) }
    var contentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val scope = rememberCoroutineScope()

    fun currentBounds(): Rect {
        val coords = contentCoords
        if (coords == null || !coords.isAttached) return Rect.Zero
        return coords.boundsInRoot().takeUnless { it.isEmpty } ?: Rect.Zero
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

    val leftOffset = animatedX.coerceIn(-MAX_DRAG, 0f)
    val rightOffset = animatedX.coerceIn(0f, MAX_DRAG)
    val rightDrag = rightOffset
    val leftDrag = (-leftOffset).coerceIn(0f, MAX_DRAG)
    val rightProg = (rightDrag / TRIGGER_AT).coerceIn(0f, 1f)
    val leftProg = (leftDrag / TRIGGER_AT).coerceIn(0f, 1f)

    val popMult = if (popping) POP_SCALE else 1f
    val replyScale = (0.5f + 0.5f * rightProg) * if (popping && popReply == true) popMult else 1f
    val optionsScale = (0.5f + 0.5f * leftProg) * if (popping && popReply == false) popMult else 1f
    val replyParallax = (-rightDrag * ICON_PARALLAX).coerceIn(-ICON_MAX_OUTWARD, 0f)
    val optionsParallax = (leftDrag * ICON_PARALLAX).coerceIn(0f, ICON_MAX_OUTWARD)
    val replyOpacity = if (popping && popReply == true) 1f else rightProg
    val optionsOpacity = if (popping && popReply == false) 1f else leftProg

    fun settle() {
        dragging = false
        popping = false
        popReply = null
        triggered = false
        dragX = 0f
        onDragProgress?.invoke(0f)
    }

    /** Never call action callbacks synchronously from [pointerInput] — that crashes Compose. */
    fun fireSwipe(reply: Boolean) {
        if (triggered) return
        triggered = true
        popping = true
        popReply = reply
        val bounds = currentBounds()
        scope.launch {
            delay(CALLBACK_DELAY_MS)
            if (reply) {
                onSwipeReply()
            } else {
                onSwipeOptions(bounds)
            }
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
                icon = LabIconName.Reply,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = replyIconInset.dp + replyParallax.dp),
                scale = replyScale,
                alpha = replyOpacity,
                popping = popping && popReply == true,
            )
        }
        if (optionsOpacity > 0.01f) {
            SwipeRevealIcon(
                icon = LabIconName.Options,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-optionsIconInset).dp + optionsParallax.dp),
                scale = optionsScale,
                alpha = optionsOpacity,
                popping = popping && popReply == false,
            )
        }

        Box(
            modifier = Modifier
                .wrapContentWidth()
                .offset { IntOffset(leftOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id
                        val start = down.position
                        var horizontalDrag = false
                        var childHandled = down.isConsumed

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
                                    if (!horizontalDrag &&
                                        !childHandled &&
                                        !change.isConsumed &&
                                        abs(dx) < TAP_SLOP_PX &&
                                        abs(dy) < TAP_SLOP_PX
                                    ) {
                                        val bounds = currentBounds()
                                        scope.launch { onTap?.invoke(bounds) }
                                    }
                                    scope.launch { settle() }
                                }
                                break
                            }

                            val dx = change.position.x - start.x
                            val dy = change.position.y - start.y

                            if (!horizontalDrag) {
                                if (abs(dx) < DRAG_START_PX && abs(dy) < DRAG_START_PX) continue
                                if (abs(dx) <= abs(dy)) break
                                horizontalDrag = true
                                dragging = true
                                triggered = false
                                popping = false
                                popReply = null
                            }

                            change.consume()
                            dragX = dx.coerceIn(-MAX_DRAG, MAX_DRAG)
                            onDragProgress?.invoke(dragX)
                            if (abs(dragX) >= TRIGGER_AT) {
                                fireSwipe(dragX > 0f)
                            }
                        }
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(rightOffset.roundToInt(), 0) }
                    .onGloballyPositioned { contentCoords = it },
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SwipeRevealIcon(
    icon: LabIconName,
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
            .background(LabColors.Gray33),
        contentAlignment = Alignment.Center,
    ) {
        LabIcon(icon, ICON_GLYPH.dp, LabColors.White33)
    }
}
