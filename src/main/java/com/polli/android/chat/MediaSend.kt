package com.polli.android.chat

import android.content.Context
import android.net.Uri
import com.polli.android.data.engine.PolliRepositories
import com.polli.android.platform.EngineBlobStore
import com.polli.android.platform.PlatformMedia

object MediaSend {
    fun sendUri(
        context: Context,
        chatId: Int,
        uri: Uri,
        mimeType: String?,
        caption: String? = null,
    ): Int? {
        val messages = PolliRepositories.messages(context)
        val resolved = mimeType ?: PlatformMedia.mimeType(context, uri) ?: "application/octet-stream"
        val path = EngineBlobStore.copyUriToBlobdir(context, uri, "file", null)
        val viewType =
            when {
                PlatformMedia.isGif(resolved) -> "Gif"
                PlatformMedia.isImageType(resolved) -> "Image"
                PlatformMedia.isVideoType(resolved) -> "Video"
                PlatformMedia.isAudioType(resolved) -> "Audio"
                else -> "File"
            }
        return messages.sendMedia(
            chatId = chatId,
            filePath = path,
            fileName = null,
            mimeType = resolved,
            caption = caption,
            viewType = viewType,
        )
    }

    fun sendVoice(context: Context, chatId: Int, uri: Uri, @Suppress("UNUSED_PARAMETER") size: Long): Int? {
        val messages = PolliRepositories.messages(context)
        return try {
            val path = EngineBlobStore.copyUriToBlobdir(context, uri, "voice", ".m4a")
            messages.sendMedia(
                chatId = chatId,
                filePath = path,
                fileName = "voice.m4a",
                mimeType = PlatformMedia.audioM4a,
                caption = null,
                viewType = "Voice",
            )
        } finally {
            PlatformMedia.deletePersistentBlob(context, uri)
        }
    }
}
