package com.polli.android.chat

import android.content.Context
import android.net.Uri
import com.b44t.messenger.DcMsg
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.providers.PersistentBlobProvider
import org.thoughtcrime.securesms.util.MediaUtil

object MediaSend {
    fun sendUri(
        context: Context,
        chatId: Int,
        uri: Uri,
        mimeType: String?,
        caption: String? = null,
    ) {
        val dc = DcHelper.getContext(context)
        val resolved = mimeType ?: MediaUtil.getMimeType(context, uri) ?: "application/octet-stream"
        val path = DcHelper.copyToBlobdir(context, uri, "file", null)
        val msgType = when {
            MediaUtil.isGif(resolved) -> DcMsg.DC_MSG_GIF
            MediaUtil.isImageType(resolved) -> DcMsg.DC_MSG_IMAGE
            MediaUtil.isVideoType(resolved) -> DcMsg.DC_MSG_VIDEO
            MediaUtil.isAudioType(resolved) -> DcMsg.DC_MSG_AUDIO
            else -> DcMsg.DC_MSG_FILE
        }
        val msg = DcMsg(dc, msgType)
        msg.setFileAndDeduplicate(path, null, resolved)
        caption?.trim()?.takeIf { it.isNotEmpty() }?.let { msg.setText(it) }
        dc.sendMsg(chatId, msg)
    }

    fun sendVoice(context: Context, chatId: Int, uri: Uri, @Suppress("UNUSED_PARAMETER") size: Long) {
        val dc = DcHelper.getContext(context)
        try {
            val path = DcHelper.copyToBlobdir(context, uri, "voice", ".m4a")
            val msg = DcMsg(dc, DcMsg.DC_MSG_VOICE)
            msg.setFileAndDeduplicate(path, null, MediaUtil.AUDIO_M4A)
            dc.sendMsg(chatId, msg)
        } finally {
            PersistentBlobProvider.getInstance().delete(context, uri)
        }
    }
}
