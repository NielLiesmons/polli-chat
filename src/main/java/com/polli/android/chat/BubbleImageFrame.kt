package com.polli.android.chat

import android.graphics.BitmapFactory
import android.widget.ImageView
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
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
 * Image is fit-center inside the frame; letterbox gaps use [LabColors.Black16].
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
            .background(LabColors.Black16)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier
                .width(contentWidth)
                .height(frameHeight),
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                    contentDescription?.let { this.contentDescription = it }
                }
            },
            update = { view ->
                Glide.with(view)
                    .load(model)
                    .fitCenter()
                    .into(view)
            },
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

    Box(
        modifier = modifier
            .width(contentWidth)
            .height(frameHeight)
            .clip(RoundedCornerShape(IMAGE_CORNER))
            .background(LabColors.Black16)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier
                .width(contentWidth)
                .height(frameHeight),
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                }
            },
            update = { view ->
                Glide.with(view)
                    .asBitmap()
                    .load(file)
                    .frame(1_000_000)
                    .fitCenter()
                    .into(view)
            },
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(LabColors.Black.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Text(
                text = "▶",
                color = LabColors.White85,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/** Landscape hugs width; portrait uses same width with taller (capped) height. */
private fun bubbleImageHeight(contentWidth: Dp, aspectRatio: Float): Dp {
    val naturalHeight = contentWidth / aspectRatio
    return minOf(naturalHeight, LabDimens.ChatBubbleImageMaxHeight)
}
