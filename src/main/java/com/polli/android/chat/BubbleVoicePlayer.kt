package com.polli.android.chat

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Observer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b44t.messenger.DcMsg
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.theme.LabColors
import com.polli.android.theme.accent
import org.thoughtcrime.securesms.components.audioplay.AudioPlaybackState
import org.thoughtcrime.securesms.components.audioplay.AudioPlaybackViewModel
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.mms.AudioSlide
import kotlin.math.roundToInt

@Composable
fun BubbleVoicePlayer(
    messageId: Int,
    isOutgoing: Boolean,
    playbackViewModel: AudioPlaybackViewModel?,
    modifier: Modifier = Modifier,
) {
    if (playbackViewModel == null) {
        MediaFallbackChip(
            label = "Voice message",
            onClick = {},
            modifier = modifier.width(200.dp),
        )
        return
    }

    val context = LocalContext.current
    val audioUri = remember(messageId) {
        val dcMsg = DcHelper.getContext(context).getMsg(messageId)
        if (dcMsg.isOk) AudioSlide(context, dcMsg).getUri() else null
    }
    var playbackState by remember { mutableStateOf(AudioPlaybackState.idle()) }
    var durations by remember { mutableStateOf<Map<Int, Long>>(emptyMap()) }

    DisposableEffect(playbackViewModel) {
        val stateObserver = Observer<AudioPlaybackState> { playbackState = it }
        val durationObserver = Observer<Map<Int, Long>> { durations = it }
        playbackViewModel.playbackState.observeForever(stateObserver)
        playbackViewModel.durations.observeForever(durationObserver)
        onDispose {
            playbackViewModel.playbackState.removeObserver(stateObserver)
            playbackViewModel.durations.removeObserver(durationObserver)
        }
    }

    LaunchedEffect(messageId, audioUri) {
        if (audioUri != null) {
            playbackViewModel.ensureDurationLoaded(context, messageId, audioUri)
        }
    }

    val durationMs = durations[messageId] ?: 0L
    val isActive = playbackState.msgId == messageId
    val isPlaying = isActive && playbackState.status == AudioPlaybackState.PlaybackStatus.PLAYING
    val positionMs = if (isActive) playbackState.currentPosition else 0L
    val displayDuration = when {
        durationMs > 0L -> durationMs
        isActive && playbackState.duration > 0L -> playbackState.duration
        else -> fallbackDurationMs(context, messageId)
    }

    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    val sliderMax = (displayDuration.coerceAtLeast(1L)).toFloat()
    val sliderValue = when {
        isSeeking -> seekValue
        displayDuration > 0L -> positionMs.coerceIn(0L, displayDuration).toFloat()
        else -> 0f
    }

    val iconBg = if (isOutgoing) LabColors.White.copy(alpha = 0.22f) else LabColors.Black33
    val iconTint = LabColors.White
    val trackColor = if (isOutgoing) LabColors.White66 else accent().light
    val timeColor = if (isOutgoing) LabColors.White66 else LabColors.White33

    Row(
        modifier = modifier
            .width(220.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = audioUri != null,
                    onClick = {
                        val uri = audioUri ?: return@clickable
                        if (isActive &&
                            (playbackState.status == AudioPlaybackState.PlaybackStatus.PLAYING ||
                                playbackState.status == AudioPlaybackState.PlaybackStatus.PAUSED)
                        ) {
                            if (playbackState.status == AudioPlaybackState.PlaybackStatus.PLAYING) {
                                playbackViewModel.pause(messageId)
                            } else {
                                playbackViewModel.play(messageId)
                            }
                        } else {
                            playbackViewModel.loadAudioAndPlay(messageId, uri)
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            LabIcon(
                if (isPlaying) LabIconName.Pause else LabIconName.Play,
                18.dp,
                iconTint,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Slider(
                value = sliderValue,
                onValueChange = {
                    isSeeking = true
                    seekValue = it
                },
                onValueChangeFinished = {
                    if (isSeeking) {
                        playbackViewModel.seekTo(seekValue.roundToInt().toLong(), messageId)
                        isSeeking = false
                    }
                },
                valueRange = 0f..sliderMax,
                enabled = displayDuration > 0L && audioUri != null,
                modifier = Modifier.height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = trackColor,
                    activeTrackColor = trackColor,
                    inactiveTrackColor = if (isOutgoing) LabColors.White16 else LabColors.White11,
                ),
            )
            Text(
                text = formatVoiceTime(positionMs, displayDuration),
                color = timeColor,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun MediaFallbackChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = LabColors.White85,
        fontSize = 14.sp,
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .background(LabColors.Black33)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
    )
}

private fun formatVoiceTime(positionMs: Long, durationMs: Long): String {
    fun fmt(ms: Long): String {
        val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }
    return if (durationMs > 0L) "${fmt(positionMs)} / ${fmt(durationMs)}" else fmt(positionMs)
}

private fun fallbackDurationMs(context: android.content.Context, messageId: Int): Long {
    val dcMsg = DcHelper.getContext(context).getMsg(messageId)
    if (!dcMsg.isOk) return 0L
    val d = dcMsg.duration
    return if (d > 0) d * 1000L else 0L
}
