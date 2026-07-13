package com.polli.android.chat

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Observer
import com.polli.android.data.engine.PolliRepositories
import com.polli.android.icons.PolliIconName
import com.polli.android.platform.PolliAudioPlaybackState
import com.polli.android.platform.PolliAudioPlaybackViewModel
import com.polli.android.platform.PolliPlaybackStatus
import com.polli.domain.model.chat.ChatMessage
import com.polli.ui.components.audio.generateWaveformBars
import com.polli.ui.util.formatAudioDurationMs
import kotlin.math.roundToInt

/** View port of [VoiceMessageBubble] + [BubbleVoicePlayer] logic. */
internal class ViewVoiceBubble(context: Context) : LinearLayout(context) {
    private val playButton = FrameLayout(context)
    private val playIcon = ImageView(context)
    private val waveform = AudioWaveformView(context)
    private val timeLabel = TextView(context)
    private var messageId = -1
    private var audioUri: Uri? = null
    private var playbackViewModel: PolliAudioPlaybackViewModel? = null
    private var isOutgoing = false

    private val stateObserver = Observer<PolliAudioPlaybackState> { refreshUi() }
    private val durationObserver = Observer<Map<Int, Long>> { refreshUi() }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val playSize = ViewChatUi.dp(context, 36f)
        val playBg =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
            }
        playButton.background = playBg
        playIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
        playButton.addView(
            playIcon,
            FrameLayout.LayoutParams(ViewChatUi.dp(context, 14f), ViewChatUi.dp(context, 14f), Gravity.CENTER),
        )
        timeLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        timeLabel.gravity = Gravity.END
        val timeW = ViewChatUi.dp(context, 36f)
        addView(playButton, LayoutParams(playSize, playSize))
        addView(
            waveform,
            LayoutParams(0, ViewChatUi.dp(context, 32f), 1f).apply {
                marginStart = ViewChatUi.dp(context, 8f)
                marginEnd = ViewChatUi.dp(context, 8f)
            },
        )
        addView(timeLabel, LayoutParams(timeW, LayoutParams.WRAP_CONTENT))
        setPadding(0, ViewChatUi.dp(context, 2f), 0, ViewChatUi.dp(context, 2f))
        minimumWidth = ViewChatUi.dp(context, 180f)
    }

    fun bind(message: ChatMessage, outgoing: Boolean, playbackVm: PolliAudioPlaybackViewModel?) {
        messageId = message.id
        isOutgoing = outgoing
        if (playbackViewModel !== playbackVm) {
            detachObservers()
            playbackViewModel = playbackVm
            if (isAttachedToWindow) attachObservers()
        }
        audioUri =
            message.filePath?.takeIf { it.isNotBlank() }?.let(Uri::parse)
                ?: PolliRepositories.messages(context).getMessage(messageId)?.filePath
                    ?.takeIf { it.isNotBlank() }
                    ?.let(Uri::parse)
        waveform.bars = generateWaveformBars(messageId, 64)
        if (audioUri != null && playbackViewModel != null) {
            playbackViewModel?.ensureDurationLoaded(context, messageId, audioUri!!)
        }
        playButton.setOnClickListener { onPlayPause() }
        refreshUi()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachObservers()
        refreshUi()
    }

    override fun onDetachedFromWindow() {
        detachObservers()
        super.onDetachedFromWindow()
    }

    private fun attachObservers() {
        playbackViewModel?.playbackState?.observeForever(stateObserver)
        playbackViewModel?.durations?.observeForever(durationObserver)
    }

    private fun detachObservers() {
        playbackViewModel?.playbackState?.removeObserver(stateObserver)
        playbackViewModel?.durations?.removeObserver(durationObserver)
    }

    private fun onPlayPause() {
        val vm = playbackViewModel ?: return
        val uri = audioUri ?: return
        val state = vm.playbackState.value ?: PolliAudioPlaybackState.idle()
        if (state.msgId == messageId &&
            (state.status == PolliPlaybackStatus.PLAYING || state.status == PolliPlaybackStatus.PAUSED)
        ) {
            if (state.status == PolliPlaybackStatus.PLAYING) vm.pause(messageId) else vm.play(messageId)
        } else {
            vm.loadAudioAndPlay(messageId, uri)
        }
    }

    private fun refreshUi() {
        val vm = playbackViewModel
        val state = vm?.playbackState?.value ?: PolliAudioPlaybackState.idle()
        val durations = vm?.durations?.value.orEmpty()
        val repoDuration = PolliRepositories.messages(context).getMessage(messageId)?.durationMs?.toLong()
        val hint = messageId.takeIf { it > 0 }?.let { id ->
            PolliRepositories.messages(context).getMessage(id)?.durationMs?.toLong()
        }
        val durationMs =
            when {
                durations[messageId]?.let { it > 0L } == true -> durations[messageId]!!
                hint?.let { it > 0L } == true -> hint
                repoDuration?.let { it > 0L } == true -> repoDuration
                state.msgId == messageId && state.duration > 0L -> state.duration
                else -> 0L
            }
        val isActive = state.msgId == messageId
        val isPlaying = isActive && state.status == PolliPlaybackStatus.PLAYING
        val positionMs = if (isActive) state.currentPosition else 0L
        val progress =
            if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        val isLoading = audioUri != null && durationMs <= 0L
        val enabled = audioUri != null

        (playButton.background as? GradientDrawable)?.setColor(
            if (isOutgoing) 0x38FFFFFF else 0x55000000,
        )
        playIcon.setImageResource(if (isPlaying) PolliIconName.Pause.resId else PolliIconName.Play.resId)
        playIcon.setColorFilter(if (enabled && !isLoading) 0xFFFFFFFF.toInt() else ViewChatUi.whiteAlpha(0.33f))
        playButton.alpha = if (enabled && !isLoading) 1f else 0.6f
        playButton.isEnabled = enabled && !isLoading

        waveform.progress = progress
        waveform.playedColor = if (isOutgoing) ViewChatUi.whiteAlpha(0.85f) else incomingAccentColor()
        waveform.unplayedColor = if (isOutgoing) ViewChatUi.whiteAlpha(0.28f) else ViewChatUi.whiteAlpha(0.33f)
        waveform.isLoading = isLoading
        waveform.setOnSeekListener(
            if (enabled && durationMs > 0L) {
                { fraction ->
                    val target = (durationMs * fraction).roundToInt().toLong()
                    playbackViewModel?.seekTo(target, messageId)
                }
            } else {
                null
            },
        )
        waveform.invalidate()

        val remainingMs = if (durationMs > 0L) (durationMs - positionMs).coerceAtLeast(0L) else 0L
        timeLabel.text =
            when {
                isLoading -> "…"
                durationMs > 0L && isPlaying -> formatAudioDurationMs(remainingMs)
                durationMs > 0L -> formatAudioDurationMs(durationMs)
                else -> "0:00"
            }
        timeLabel.setTextColor(if (isOutgoing) ViewChatUi.whiteAlpha(0.66f) else ViewChatUi.whiteAlpha(0.33f))
    }

    private fun incomingAccentColor(): Int {
        val preset = com.polli.android.settings.AppPrefs(context).accentPreset
        val palette = com.polli.ui.theme.AccentThemes.palette(preset)
        val c = palette.gradientStart
        return android.graphics.Color.argb(
            (0.9f * 255).roundToInt(),
            (c.red * 255).roundToInt(),
            (c.green * 255).roundToInt(),
            (c.blue * 255).roundToInt(),
        )
    }
}

private class AudioWaveformView(context: Context) : View(context) {
    var bars: FloatArray = FloatArray(0)
    var progress: Float = 0f
    var playedColor: Int = 0xFFFFFFFF.toInt()
    var unplayedColor: Int = 0x55FFFFFF
    var isLoading: Boolean = false
    private var onSeek: ((Float) -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val barWidthPx = ViewChatUi.dp(context, 2.8f).toFloat()
    private val barGapPx = ViewChatUi.dp(context, 2.4f).toFloat()

    fun setOnSeekListener(listener: ((Float) -> Unit)?) {
        onSeek = listener
        isClickable = listener != null
    }

    init {
        setOnClickListener {
            val w = width.toFloat()
            if (w <= 0f) return@setOnClickListener
            onSeek?.invoke((it.x / w).coerceIn(0f, 1f))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val numLines = ((width) / (barWidthPx + barGapPx)).toInt().coerceAtLeast(1)
        val actualSpacing =
            if (numLines <= 1) 0f else (width - numLines * barWidthPx) / (numLines - 1)
        val centerY = height / 2f
        val progressClamped = progress.coerceIn(0f, 1f)
        paint.strokeWidth = barWidthPx
        for (i in 0 until numLines) {
            val barIndex =
                if (bars.isEmpty()) 0 else (i * bars.size / numLines).coerceIn(0, bars.lastIndex)
            val amplitude = if (bars.isEmpty()) 0.5f else bars[barIndex]
            val barHeight = (amplitude * height * 0.8f).coerceIn(1f, height * 0.8f)
            val x = i * (barWidthPx + actualSpacing) + barWidthPx / 2f
            val played = (i.toFloat() / numLines) <= progressClamped
            paint.color = if (played) playedColor else unplayedColor
            canvas.drawLine(x, centerY - barHeight / 2f, x, centerY + barHeight / 2f, paint)
        }
    }
}
