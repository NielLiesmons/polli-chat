package com.polli.android.chat

import android.content.Context
import android.net.Uri
import com.polli.android.data.engine.PolliRepositories
import com.polli.android.platform.EngineBlobStore
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
        val messages = PolliRepositories.messages(context)
        val resolved = mimeType ?: MediaUtil.getMimeType(context, uri) ?: "application/octet-stream"
        val path = EngineBlobStore.copyUriToBlobdir(context, uri, "file", null)
        val viewType =
            when {
                MediaUtil.isGif(resolved) -> "Gif"
                MediaUtil.isImageType(resolved) -> "Image"
                MediaUtil.isVideoType(resolved) -> "Video"
                MediaUtil.isAudioType(resolved) -> "Audio"
                else -> "File"
            }
        messages.sendMedia(
            chatId = chatId,
            filePath = path,
            fileName = null,
            mimeType = resolved,
            caption = caption,
            viewType = viewType,
        )
    }

    fun sendVoice(context: Context, chatId: Int, uri: Uri, @Suppress("UNUSED_PARAMETER") size: Long) {
        val messages = PolliRepositories.messages(context)
        try {
            val path = EngineBlobStore.copyUriToBlobdir(context, uri, "voice", ".m4a")
            messages.sendMedia(
                chatId = chatId,
                filePath = path,
                fileName = "voice.m4a",
                mimeType = MediaUtil.AUDIO_M4A,
                caption = null,
                viewType = "Voice",
            )
        } finally {
            PersistentBlobProvider.getInstance().delete(context, uri)
        }
    }
}
