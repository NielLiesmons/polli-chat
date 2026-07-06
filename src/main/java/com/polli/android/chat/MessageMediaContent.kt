package com.polli.android.chat

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
import com.polli.domain.model.chat.ChatMessage
import com.polli.android.theme.PolliColors
import com.polli.android.navigation.AppNav
import java.io.File

@Composable
fun DcMsgMediaContent(
    msg: DcMsg,
    contentWidth: Dp = 320.dp,
    modifier: Modifier = Modifier,
) {
    MessageMediaContent(
        messageId = msg.id,
        viewType = dcViewTypeName(msg.type),
        fileName = msg.filename,
        filePath = msg.file?.takeIf { it.isNotBlank() },
        width = msg.getWidth(0).takeIf { it > 0 },
        height = msg.getHeight(0).takeIf { it > 0 },
        durationMs = msg.duration.takeIf { it > 0 }?.times(1000),
        contentWidth = contentWidth,
        isOutgoing = msg.isOutgoing,
        modifier = modifier,
    )
}

@Composable
fun MessageMediaContent(
    message: ChatMessage,
    contentWidth: Dp,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier,
) {
    MessageMediaContent(
        messageId = message.id,
        viewType = message.viewType,
        fileName = message.fileName,
        filePath = message.filePath,
        width = message.width,
        height = message.height,
        durationMs = message.durationMs,
        contentWidth = contentWidth,
        isOutgoing = isOutgoing,
        modifier = modifier,
    )
}

@Composable
private fun MessageMediaContent(
    messageId: Int,
    viewType: String,
    fileName: String?,
    filePath: String?,
    width: Int?,
    height: Int?,
    durationMs: Int?,
    contentWidth: Dp,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier,
) {
    val playbackViewModel = LocalChatAudioPlayback.current
    val context = LocalContext.current
    val file = remember(filePath) {
        filePath?.takeIf { it.isNotBlank() }?.let(::File)
    }
    val aspectRatio = remember(width, height, file) {
        width?.let { w -> height?.let { h -> aspectRatioFromPixels(w, h) } }
            ?: file?.let(::aspectRatioFromFile)
    }
    val voiceDurationHint = remember(durationMs) { durationMs?.toLong() }

    when (viewType) {
        "Image",
        "Gif",
        "Sticker",
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
        "Video" -> {
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
        "Voice" ->
            BubbleVoicePlayer(
                messageId = messageId,
                isOutgoing = isOutgoing,
                playbackViewModel = playbackViewModel,
                durationMsHint = voiceDurationHint,
                waveformSeed = messageId,
                modifier = modifier,
            )
        "Audio" -> MediaFallbackChip(
            label = fileName ?: "Audio",
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
        color = PolliColors.White85,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PolliColors.Black33)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
    )
}

private fun openMediaPreview(context: android.content.Context, messageId: Int) {
    AppNav.openMediaPreview(context, messageId)
}

private fun dcViewTypeName(type: Int): String =
    when (type) {
        DcMsg.DC_MSG_TEXT -> "Text"
        DcMsg.DC_MSG_IMAGE -> "Image"
        DcMsg.DC_MSG_GIF -> "Gif"
        DcMsg.DC_MSG_STICKER -> "Sticker"
        DcMsg.DC_MSG_AUDIO -> "Audio"
        DcMsg.DC_MSG_VOICE -> "Voice"
        DcMsg.DC_MSG_VIDEO -> "Video"
        DcMsg.DC_MSG_FILE -> "File"
        DcMsg.DC_MSG_CALL -> "Call"
        DcMsg.DC_MSG_WEBXDC -> "Webxdc"
        DcMsg.DC_MSG_VCARD -> "Vcard"
        else -> "Text"
    }
