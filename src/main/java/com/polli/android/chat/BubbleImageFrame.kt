package com.polli.android.chat

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens
import java.io.File

private val IMAGE_CORNER = 8.dp
private const val DEFAULT_ASPECT_RATIO = 4f / 3f
private const val MIN_ASPECT_RATIO = 0.35f
private const val MAX_ASPECT_RATIO = 2.8f

/** width / height */
fun aspectRatioFromPixels(width: Int, height: Int): Float? =
    if (width > 0 && height > 0) width.toFloat() / height.toFloat() else null

fun aspectRatioFromFile(file: File): Float? {
    if (!file.exists()) return null
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, opts)
    return aspectRatioFromPixels(opts.outWidth, opts.outHeight)
}

/**
 * Bubble media image: full [contentWidth], height from aspect ratio (capped).
 * Image is fit-center inside the frame; letterbox gaps use [PolliColors.Black16].
 */
@Composable
fun BubbleImageFrame(
    contentWidth: Dp,
    aspectRatio: Float,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    model: Any,
    contentDescription: String? = null,
) {
    val ratio = aspectRatio.coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
    val frameHeight = remember(contentWidth, ratio) {
        bubbleImageHeight(contentWidth, ratio)
    }

    Box(
        modifier = modifier
            .width(contentWidth)
            .height(frameHeight)
            .clip(RoundedCornerShape(IMAGE_CORNER))
            .background(PolliColors.Black16)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(contentWidth)
                .height(frameHeight),
        )
    }
}

@Composable
fun BubbleVideoThumbnailFrame(
    file: File,
    contentWidth: Dp,
    aspectRatio: Float?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratio = (aspectRatio ?: aspectRatioFromFile(file) ?: DEFAULT_ASPECT_RATIO)
        .coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
    val frameHeight = remember(contentWidth, ratio) {
        bubbleImageHeight(contentWidth, ratio)
    }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .width(contentWidth)
            .height(frameHeight)
            .clip(RoundedCornerShape(IMAGE_CORNER))
            .background(PolliColors.Black16)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = remember(file) {
                ImageRequest.Builder(context)
                    .data(file)
                    .videoFrameMillis(1000)
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .build()
            },
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(contentWidth)
                .height(frameHeight),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(PolliColors.Black.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Text(
                text = "▶",
                color = PolliColors.White85,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/** Landscape hugs width; portrait uses same width with taller (capped) height. */
private fun bubbleImageHeight(contentWidth: Dp, aspectRatio: Float): Dp {
    val naturalHeight = contentWidth / aspectRatio
    return minOf(naturalHeight, PolliDimens.ChatBubbleImageMaxHeight)
}
