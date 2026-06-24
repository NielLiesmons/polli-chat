package com.polli.android.chat

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.b44t.messenger.DcMsg
import com.polli.android.theme.LabColors
import org.thoughtcrime.securesms.MediaPreviewActivity

@Composable
fun DcMsgMediaContent(msg: DcMsg, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val label = when (msg.type) {
        DcMsg.DC_MSG_IMAGE -> "Photo"
        DcMsg.DC_MSG_GIF -> "GIF"
        DcMsg.DC_MSG_VIDEO -> "Video"
        DcMsg.DC_MSG_STICKER -> "Sticker"
        DcMsg.DC_MSG_AUDIO, DcMsg.DC_MSG_VOICE -> "Voice message"
        DcMsg.DC_MSG_FILE -> msg.filename ?: "File"
        else -> "Attachment"
    }
    Text(
        text = label,
        color = LabColors.White85,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { openMediaPreview(context, msg.id) }
            .padding(8.dp),
    )
}

@Composable
fun MessageMediaContent(message: ChatMessage, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val label = when (message.viewType) {
        DcMsg.DC_MSG_IMAGE -> "📷 Photo"
        DcMsg.DC_MSG_GIF -> "GIF"
        DcMsg.DC_MSG_VIDEO -> "🎬 Video"
        DcMsg.DC_MSG_STICKER -> "Sticker"
        DcMsg.DC_MSG_AUDIO, DcMsg.DC_MSG_VOICE -> "🎵 Voice message"
        DcMsg.DC_MSG_FILE -> "📎 ${message.fileName ?: "File"}"
        else -> "Attachment"
    }
    Text(
        text = label,
        color = LabColors.White85,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { openMediaPreview(context, message.id) }
            .padding(8.dp),
    )
}

private fun openMediaPreview(context: android.content.Context, messageId: Int) {
    context.startActivity(
        Intent(context, MediaPreviewActivity::class.java).apply {
            putExtra(MediaPreviewActivity.DC_MSG_ID, messageId)
        },
    )
}
