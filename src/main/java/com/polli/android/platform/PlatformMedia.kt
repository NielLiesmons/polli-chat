package com.polli.android.platform

import android.content.Context
import android.net.Uri
import org.thoughtcrime.securesms.providers.PersistentBlobProvider
import org.thoughtcrime.securesms.util.MediaUtil

object PlatformMedia {
    fun mimeType(context: Context, uri: Uri): String? = MediaUtil.getMimeType(context, uri)

    fun isGif(mime: String): Boolean = MediaUtil.isGif(mime)

    fun isImageType(mime: String): Boolean = MediaUtil.isImageType(mime)

    fun isVideoType(mime: String): Boolean = MediaUtil.isVideoType(mime)

    fun isAudioType(mime: String): Boolean = MediaUtil.isAudioType(mime)

    val audioM4a: String get() = MediaUtil.AUDIO_M4A

    fun createPersistentBlob(
        context: Context,
        data: ByteArray,
        mimeType: String,
        filename: String,
    ): Uri = PersistentBlobProvider.getInstance().create(context, data, mimeType, filename)

    fun createPersistentBlobFromStream(
        context: Context,
        inputStream: java.io.InputStream,
        mimeType: String?,
        filename: String?,
        fileSize: Long?,
    ): Uri =
        PersistentBlobProvider.getInstance().create(
            context,
            inputStream,
            mimeType ?: "application/octet-stream",
            filename,
            fileSize,
        )

    fun isLocalAttachmentUri(uri: Uri): Boolean =
        org.thoughtcrime.securesms.mms.PartAuthority.isLocalUri(uri)

    fun deletePersistentBlob(context: Context, uri: Uri) {
        PersistentBlobProvider.getInstance().delete(context, uri)
    }
}
