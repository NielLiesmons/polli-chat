package com.polli.android.chat

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import coil.decode.VideoFrameDecoder
import coil.load
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.polli.android.navigation.AppNav
import com.polli.android.platform.PolliAudioPlaybackViewModel
import com.polli.android.theme.PolliDimens
import com.polli.domain.model.chat.ChatMessage
import java.io.File
import kotlin.math.roundToInt

/** View bind path for bubble attachments — mirrors [MessageMediaContent]. */
internal object ViewBubbleMedia {
    private const val DEFAULT_ASPECT = 4f / 3f
    private const val MIN_ASPECT = 0.35f
    private const val MAX_ASPECT = 2.8f

    fun bind(
        host: FrameLayout,
        message: ChatMessage,
        contentWidthPx: Int,
        isOutgoing: Boolean,
        playbackViewModel: PolliAudioPlaybackViewModel?,
        horizontalPadPx: Int,
    ) {
        host.removeAllViews()
        if (!message.hasAttachment) {
            host.visibility = View.GONE
            return
        }
        host.visibility = View.VISIBLE
        host.setPadding(horizontalPadPx, 0, horizontalPadPx, ViewChatUi.dp(host.context, 4f))

        val file = message.filePath?.takeIf { it.isNotBlank() }?.let(::File)
        val aspect =
            aspectRatioFromPixels(message.width, message.height)
                ?: file?.let(::aspectRatioFromFile)
                ?: DEFAULT_ASPECT

        when (message.viewType) {
            "Image", "Gif", "Sticker" ->
                if (file != null && file.exists()) {
                    addImageFrame(host, file, contentWidthPx, aspect, message.id, message.fileName ?: "Image")
                } else {
                    addFallbackChip(host, message.fileName ?: "Photo", contentWidthPx, message.id)
                }
            "Video" ->
                if (file != null && file.exists()) {
                    addVideoFrame(host, file, contentWidthPx, aspect, message.id)
                } else {
                    addFallbackChip(host, message.fileName ?: "Video", contentWidthPx, message.id)
                }
            "Voice" -> {
                val voice = ViewVoiceBubble(host.context)
                voice.bind(message, isOutgoing, playbackViewModel)
                host.addView(voice, FrameLayout.LayoutParams(contentWidthPx.coerceAtLeast(minWidthPx(host.context)), FrameLayout.LayoutParams.WRAP_CONTENT))
            }
            "Audio", "File" ->
                addFallbackChip(
                    host,
                    message.fileName ?: if (message.viewType == "Audio") "Audio" else "Attachment",
                    contentWidthPx.coerceAtLeast(minWidthPx(host.context)),
                    message.id,
                )
            else ->
                addFallbackChip(host, message.fileName ?: message.viewType, contentWidthPx, message.id)
        }
    }

    private fun addImageFrame(
        host: FrameLayout,
        file: File,
        contentWidthPx: Int,
        aspect: Float,
        messageId: Int,
        label: String,
    ) {
        val heightPx = frameHeightPx(host.context, contentWidthPx, aspect)
        val frame = mediaFrame(host.context, contentWidthPx, heightPx, messageId)
        val image = ImageView(host.context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = label
            load(file) { crossfade(false) }
        }
        frame.addView(image, FrameLayout.LayoutParams(contentWidthPx, heightPx, Gravity.CENTER))
        host.addView(frame, FrameLayout.LayoutParams(contentWidthPx, heightPx))
    }

    private fun addVideoFrame(
        host: FrameLayout,
        file: File,
        contentWidthPx: Int,
        aspect: Float,
        messageId: Int,
    ) {
        val ratio = (aspectRatioFromFile(file) ?: aspect).coerceIn(MIN_ASPECT, MAX_ASPECT)
        val heightPx = frameHeightPx(host.context, contentWidthPx, ratio)
        val frame = mediaFrame(host.context, contentWidthPx, heightPx, messageId)
        val image = ImageView(host.context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            load(
                ImageRequest.Builder(context)
                    .data(file)
                    .videoFrameMillis(1000)
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .build(),
            ) { crossfade(false) }
        }
        frame.addView(image, FrameLayout.LayoutParams(contentWidthPx, heightPx, Gravity.CENTER))
        val play = TextView(host.context).apply {
            text = "▶"
            setTextColor(ViewChatUi.whiteAlpha(0.85f))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
            setBackgroundColor(0x26000000)
        }
        frame.addView(play, FrameLayout.LayoutParams(contentWidthPx, heightPx, Gravity.CENTER))
        host.addView(frame, FrameLayout.LayoutParams(contentWidthPx, heightPx))
    }

    private fun addFallbackChip(host: FrameLayout, label: String, widthPx: Int, messageId: Int) {
        val chip =
            TextView(host.context).apply {
                text = label
                setTextColor(ViewChatUi.whiteAlpha(0.85f))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.DEFAULT
                setPadding(
                    ViewChatUi.dp(context, 10f),
                    ViewChatUi.dp(context, 12f),
                    ViewChatUi.dp(context, 10f),
                    ViewChatUi.dp(context, 12f),
                )
                background =
                    GradientDrawable().apply {
                        cornerRadius = ViewChatUi.dp(context, 8f).toFloat()
                        setColor(0x55000000)
                    }
                setOnClickListener { AppNav.openMediaPreview(host.context, messageId) }
            }
        host.addView(
            chip,
            FrameLayout.LayoutParams(widthPx.coerceAtLeast(minWidthPx(host.context)), FrameLayout.LayoutParams.WRAP_CONTENT),
        )
    }

    private fun mediaFrame(context: Context, widthPx: Int, heightPx: Int, messageId: Int): FrameLayout =
        FrameLayout(context).apply {
            background =
                GradientDrawable().apply {
                    cornerRadius = ViewChatUi.dp(context, 8f).toFloat()
                    setColor(0x29000000) // Black16
                }
            setOnClickListener { AppNav.openMediaPreview(context, messageId) }
        }

    private fun frameHeightPx(context: Context, contentWidthPx: Int, aspectRatio: Float): Int {
        val ratio = aspectRatio.coerceIn(MIN_ASPECT, MAX_ASPECT)
        val natural = (contentWidthPx / ratio).roundToInt()
        val maxH = ViewChatUi.dp(context, PolliDimens.ChatBubbleImageMaxHeight.value)
        return minOf(natural, maxH)
    }

    private fun minWidthPx(context: Context): Int =
        ViewChatUi.dp(context, PolliDimens.ChatBubbleImageMinWidth.value)

    private fun aspectRatioFromPixels(width: Int?, height: Int?): Float? =
        if (width != null && height != null && width > 0 && height > 0) width.toFloat() / height else null

    private fun aspectRatioFromFile(file: File): Float? {
        if (!file.exists()) return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return aspectRatioFromPixels(opts.outWidth, opts.outHeight)
    }
}
