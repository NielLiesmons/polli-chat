package com.polli.android.chat

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Observer
import com.polli.domain.repository.MessageRepository
import com.polli.android.data.engine.PolliRepositories
import com.polli.ui.components.audio.VoiceMessageBubble
import org.thoughtcrime.securesms.components.audioplay.AudioPlaybackState
import org.thoughtcrime.securesms.components.audioplay.AudioPlaybackViewModel
import kotlin.math.roundToInt

@Composable
fun BubbleVoicePlayer(
    messageId: Int,
    isOutgoing: Boolean,
    playbackViewModel: AudioPlaybackViewModel?,
    modifier: Modifier = Modifier,
    durationMsHint: Long? = null,
    waveformSeed: Int = messageId,
) {
    if (playbackViewModel == null) {
        VoiceMessageBubble(
            isOutgoing = isOutgoing,
            isPlaying = false,
            isLoading = false,
            progress = 0f,
            durationMs = durationMsHint ?: 0L,
            positionMs = 0L,
            waveformSeed = waveformSeed,
            enabled = false,
            onPlayPause = {},
            onSeek = {},
            modifier = modifier,
        )
        return
    }

    val context = LocalContext.current
    val messages: MessageRepository = remember { PolliRepositories.messages(context) }
    val audioUri =
        remember(messageId) {
            messages.getMessage(messageId)?.filePath?.takeIf { it.isNotBlank() }?.let(Uri::parse)
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

    val repoDuration = remember(messageId) { messages.getMessage(messageId)?.durationMs?.toLong() }
    val durationMs =
        when {
            durations[messageId]?.let { it > 0L } == true -> durations[messageId]!!
            durationMsHint?.let { it > 0L } == true -> durationMsHint
            repoDuration?.let { it > 0L } == true -> repoDuration
            playbackState.msgId == messageId && playbackState.duration > 0L -> playbackState.duration
            else -> 0L
        }
    val isActive = playbackState.msgId == messageId
    val isPlaying = isActive && playbackState.status == AudioPlaybackState.PlaybackStatus.PLAYING
    val positionMs = if (isActive) playbackState.currentPosition else 0L
    val progress =
        if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    val isLoading = audioUri != null && durationMs <= 0L

    VoiceMessageBubble(
        isOutgoing = isOutgoing,
        isPlaying = isPlaying,
        isLoading = isLoading,
        progress = progress,
        durationMs = durationMs,
        positionMs = positionMs,
        waveformSeed = waveformSeed,
        enabled = audioUri != null,
        onPlayPause = {
            val uri = audioUri ?: return@VoiceMessageBubble
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
        onSeek = { fraction ->
            if (durationMs <= 0L) return@VoiceMessageBubble
            val targetMs = (durationMs * fraction).roundToInt().toLong()
            playbackViewModel.seekTo(targetMs, messageId)
        },
        modifier = modifier,
    )
}
