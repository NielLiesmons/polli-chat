package com.polli.android.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.icons.PolliIcon
import com.polli.android.icons.PolliIconName
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens
import com.polli.android.theme.accent
import com.polli.android.theme.rougeGradient
import com.polli.android.R
import kotlin.math.roundToInt

internal enum class VoiceRecordMode { Idle, Holding, Locked }

internal fun formatVoiceRecordTime(ms: Long): String {
    val tenths = ((ms / 100) % 10).toInt()
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d.%d".format(minutes, seconds, tenths)
}

/** Replaces the + button and message field inside the composer row — no shell chrome. */
@Composable
internal fun VoiceRecordingInline(
    ms: Long,
    cancelled: Boolean,
    locked: Boolean,
    dragX: Float,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cancelled) {
            Box(
                modifier = Modifier
                    .size(PolliDimens.ChatComposerPlusSize)
                    .clip(CircleShape)
                    .background(rougeGradient()),
                contentAlignment = Alignment.Center,
            ) {
                PolliIcon(PolliIconName.Cross, 16.dp, PolliColors.White66)
            }
        } else {
            Row(
                modifier = Modifier.padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecordingBlinkDot()
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = formatVoiceRecordTime(ms),
                    color = PolliColors.White85,
                    fontSize = 15.sp,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        if (locked) {
            Text(
                text = stringResource(R.string.cancel),
                color = PolliColors.White66,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        } else {
            SlideToCancelHint(
                dragOffsetX = dragX,
                modifier = Modifier.alpha(0.92f),
            )
        }
    }
}

@Composable
private fun RecordingBlinkDot() {
    val transition = rememberInfiniteTransition(label = "recordDot")
    val alpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
        label = "recordDotAlpha",
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(accent().solid.copy(alpha = alpha)),
    )
}

@Composable
private fun SlideToCancelHint(
    dragOffsetX: Float,
    modifier: Modifier = Modifier,
) {
    val chevronShift by rememberInfiniteTransition(label = "slideChevron").animateFloat(
        initialValue = 0f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 880, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "chevronShift",
    )
    val combinedShift = chevronShift + dragOffsetX.coerceIn(-72f, 0f) * 0.35f
    Row(
        modifier = modifier.offset { IntOffset(combinedShift.roundToInt(), 0) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PolliIcon(PolliIconName.ChevronLeft, 12.dp, PolliColors.White33)
        Text(
            text = stringResource(R.string.chat_record_slide_to_cancel),
            color = PolliColors.White33,
            fontSize = 13.sp,
        )
    }
}

@Composable
internal fun VoiceLockPill(
    visible: Boolean,
    dragUpPx: Float,
    modifier: Modifier = Modifier,
) {
    val lockThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 120.dp.toPx() }
    val progress = (-dragUpPx / lockThresholdPx).coerceIn(0f, 1f)
    val enterOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = tween(220),
        label = "lockEnter",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(180),
        label = "lockAlpha",
    )
    if (!visible && alpha <= 0.01f) return

    Column(
        modifier = modifier
            .alpha(alpha)
            .offset { IntOffset(0, enterOffset.roundToInt()) }
            .width(PolliDimens.ChatFloatingChromeSize)
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(PolliColors.Gray)
            .border(PolliDimens.ShellBorderWidth, PolliColors.ShellBorder, RoundedCornerShape(20.dp))
            .padding(top = 8.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        PolliIcon(PolliIconName.ArrowUp, 16.dp, PolliColors.White66.copy(alpha = 0.6f + progress * 0.4f))
        PolliIcon(PolliIconName.Lock, 18.dp, PolliColors.White85.copy(alpha = 0.65f + progress * 0.35f))
    }
}
