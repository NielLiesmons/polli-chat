package com.polli.ui.components.audio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Fast deterministic bar heights — no audio decode (Telegram-style placeholder until real peaks land). */
fun generateWaveformBars(seed: Int, barCount: Int): FloatArray {
    var state = seed.toLong() xor 0x5DEECE66DL
    return FloatArray(barCount) {
        state = state * 1_103_515_245L + 12_345L
        val sample = ((state shr 16) and 0x7FFF) / 32767f
        0.12f + sample * 0.88f
    }
}

@Composable
fun AudioWaveform(
    bars: FloatArray,
    progress: Float,
    playedColor: Color,
    unplayedColor: Color,
    modifier: Modifier = Modifier,
    barWidth: Dp = 2.8.dp,
    barGap: Dp = 2.4.dp,
    onSeek: ((Float) -> Unit)? = null,
) {
    val seekModifier =
        if (onSeek != null) {
            Modifier.pointerInput(onSeek) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
        } else {
            Modifier
        }

    Canvas(
        modifier =
            modifier
                .then(seekModifier)
                .fillMaxWidth()
                .height(32.dp),
    ) {
        val lineWidth = barWidth.toPx()
        val spacing = barGap.toPx()
        val totalUnit = lineWidth + spacing
        val numLines = ((size.width) / totalUnit).toInt().coerceAtLeast(1)
        val actualSpacing =
            if (numLines <= 1) 0f else (size.width - numLines * lineWidth) / (numLines - 1)
        val centerY = size.height / 2f
        val progressClamped = progress.coerceIn(0f, 1f)

        for (i in 0 until numLines) {
            val barIndex = (i * bars.size / numLines).coerceIn(0, bars.lastIndex)
            val amplitude = bars[barIndex]
            val barHeight = (amplitude * size.height * 0.8f).coerceIn(1f, size.height * 0.8f)
            val x = i * (lineWidth + actualSpacing)
            val y = centerY - barHeight / 2f
            val played = (i.toFloat() / numLines) <= progressClamped
            drawLine(
                color = if (played) playedColor else unplayedColor,
                start = Offset(x, y),
                end = Offset(x, y + barHeight),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Shimmer bars while duration / URI is still loading. */
@Composable
fun LoadingAudioWaveform(
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 20,
) {
    val transition = rememberInfiniteTransition(label = "voice-wave-loading")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "voice-wave-phase",
    )
    val baseBars = remember { generateWaveformBars(42, barCount) }

    Canvas(
        modifier = modifier.fillMaxWidth().height(28.dp),
    ) {
        val lineWidth = 2.8.dp.toPx()
        val spacing = 2.4.dp.toPx()
        val numLines = ((size.width) / (lineWidth + spacing)).toInt().coerceAtLeast(1)
        val actualSpacing =
            if (numLines <= 1) 0f else (size.width - numLines * lineWidth) / (numLines - 1)
        val centerY = size.height / 2f

        for (i in 0 until numLines) {
            val waveOffset = (i.toFloat() / numLines + phase) * 2f * PI.toFloat()
            val amplitude = baseBars[i % baseBars.size]
            val animated = amplitude * (0.8f + 0.2f * (1f + sin(waveOffset)))
            val barHeight = (animated * size.height * 0.8f).coerceIn(1f, size.height * 0.8f)
            val x = i * (lineWidth + actualSpacing)
            val y = centerY - barHeight / 2f
            drawLine(
                color = color,
                start = Offset(x, y),
                end = Offset(x, y + barHeight),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}
