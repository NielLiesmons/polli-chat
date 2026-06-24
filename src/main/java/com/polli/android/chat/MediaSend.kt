package com.polli.android.chat

import android.content.Context
import android.net.Uri
import com.b44t.messenger.DcMsg
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.providers.PersistentBlobProvider
import org.thoughtcrime.securesms.util.MediaUtil

object MediaSend {
    fun sendUri(context: Context, chatId: Int, uri: Uri, mimeType: String?) {
        val dc = DcHelper.getContext(context)
        val resolved = mimeType ?: MediaUtil.getMimeType(context, uri) ?: "application/octet-stream"
        val path = DcHelper.copyToBlobdir(context, uri, "file", null)
        val msg = DcMsg(dc, DcMsg.DC_MSG_FILE)
        msg.setFileAndDeduplicate(path, null, resolved)
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
