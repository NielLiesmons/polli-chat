package com.polli.ui.components.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.ui.components.PolliIcon
import com.polli.ui.components.PolliIconName
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.accent
import com.polli.ui.util.formatAudioDurationMs

/**
 * Telegram / WhatsApp style voice row — play chip + waveform bars + remaining time.
 * No Material slider. Waveform is cheap Canvas bars (see [generateWaveformBars]).
 *
 * Future: global [com.polli.ui.bridge.AppMediaPlayback] overlay for cross-screen control.
 */
@Composable
fun VoiceMessageBubble(
    isOutgoing: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: Float,
    durationMs: Long,
    positionMs: Long,
    waveformSeed: Int,
    enabled: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (fraction: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bars = remember(waveformSeed) { generateWaveformBars(waveformSeed, 64) }
    val playBg = if (isOutgoing) PolliColors.White.copy(alpha = 0.22f) else PolliColors.Black33
    val playIcon = if (enabled && !isLoading) PolliColors.White else PolliColors.White33
    val playedWave = if (isOutgoing) PolliColors.White.copy(alpha = 0.85f) else accent().light.copy(alpha = 0.9f)
    val unplayedWave = if (isOutgoing) PolliColors.White.copy(alpha = 0.28f) else PolliColors.White33
    val timeColor = if (isOutgoing) PolliColors.White66 else PolliColors.White33
    val remainingMs =
        if (durationMs > 0L) {
            (durationMs - positionMs).coerceAtLeast(0L)
        } else {
            0L
        }
    val timeLabel =
        when {
            isLoading -> "…"
            durationMs > 0L && isPlaying -> formatAudioDurationMs(remainingMs)
            durationMs > 0L -> formatAudioDurationMs(durationMs)
            else -> "0:00"
        }

    Row(
        modifier =
            modifier
                .widthIn(min = 180.dp, max = 264.dp)
                .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(playBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled && !isLoading,
                        onClick = onPlayPause,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            PolliIcon(
                icon = if (isPlaying) PolliIconName.Pause else PolliIconName.Play,
                size = 14.dp,
                color = playIcon,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                LoadingAudioWaveform(color = PolliColors.White8)
            } else {
                AudioWaveform(
                    bars = bars,
                    progress = progress,
                    playedColor = playedWave,
                    unplayedColor = unplayedWave,
                    onSeek = if (enabled && durationMs > 0L) onSeek else null,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = timeLabel,
            color = timeColor,
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp),
        )
    }
}
