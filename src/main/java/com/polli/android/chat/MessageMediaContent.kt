package com.polli.android.chat

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.b44t.messenger.DcMsg
import com.polli.android.theme.LabColors
import org.thoughtcrime.securesms.MediaPreviewActivity
import org.thoughtcrime.securesms.connect.DcHelper
import java.io.File

@Composable
fun DcMsgMediaContent(
    msg: DcMsg,
    contentWidth: Dp = 320.dp,
    modifier: Modifier = Modifier,
) {
    MessageMediaContent(
        messageId = msg.id,
        viewType = msg.type,
        fileName = msg.filename,
        contentWidth = contentWidth,
        modifier = modifier,
    )
}

@Composable
fun MessageMediaContent(
    message: ChatMessage,
    contentWidth: Dp,
    modifier: Modifier = Modifier,
) {
    MessageMediaContent(
        messageId = message.id,
        viewType = message.viewType,
        fileName = message.fileName,
        contentWidth = contentWidth,
        modifier = modifier,
    )
}

@Composable
private fun MessageMediaContent(
    messageId: Int,
    viewType: Int,
    fileName: String?,
    contentWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dcMsg = remember(messageId) {
        DcHelper.getContext(context).getMsg(messageId).takeIf { it.isOk }
    }
    val file = remember(dcMsg) {
        dcMsg?.getFile()?.takeIf { it.isNotBlank() }?.let(::File)
    }
    val aspectRatio = remember(dcMsg, file) {
        dcMsg?.let { aspectRatioFromPixels(it.getWidth(0), it.getHeight(0)) }
            ?: file?.let(::aspectRatioFromFile)
    }

    when (viewType) {
        DcMsg.DC_MSG_IMAGE,
        DcMsg.DC_MSG_GIF,
        DcMsg.DC_MSG_STICKER,
        -> {
            if (file != null && file.exists()) {
                BubbleImageFrame(
                    contentWidth = contentWidth,
                    aspectRatio = aspectRatio ?: (4f / 3f),
                    onClick = { openMediaPreview(context, messageId) },
                    modifier = modifier,
                    model = file,
                    contentDescription = fileName ?: "Image",
                )
            } else {
                MediaFallbackChip(
                    label = fileName ?: "Photo",
                    modifier = modifier,
                    onClick = { openMediaPreview(context, messageId) },
                )
            }
        }
        DcMsg.DC_MSG_VIDEO -> {
            if (file != null && file.exists()) {
                BubbleVideoThumbnailFrame(
                    file = file,
                    contentWidth = contentWidth,
                    aspectRatio = aspectRatio,
                    onClick = { openMediaPreview(context, messageId) },
                    modifier = modifier,
                )
            } else {
                MediaFallbackChip(
                    label = fileName ?: "Video",
                    modifier = modifier,
                    onClick = { openMediaPreview(context, messageId) },
                )
            }
        }
        DcMsg.DC_MSG_AUDIO,
        DcMsg.DC_MSG_VOICE,
        -> MediaFallbackChip(
            label = if (viewType == DcMsg.DC_MSG_VOICE) "Voice message" else (fileName ?: "Audio"),
            modifier = modifier.width(contentWidth.coerceAtLeast(180.dp)),
            onClick = { openMediaPreview(context, messageId) },
        )
        else -> MediaFallbackChip(
            label = fileName ?: "Attachment",
            modifier = modifier.width(contentWidth.coerceAtLeast(180.dp)),
            onClick = { openMediaPreview(context, messageId) },
        )
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
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LabColors.Black33)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
    )
}

private fun openMediaPreview(context: android.content.Context, messageId: Int) {
    context.startActivity(
        Intent(context, MediaPreviewActivity::class.java).apply {
            putExtra(MediaPreviewActivity.DC_MSG_ID, messageId)
        },
    )
}
