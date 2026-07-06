package com.polli.android.platform

import android.content.Context
import android.net.Uri
import java.io.IOException

/** Copies content URIs into the engine blob directory (thin DcHelper wrapper). */
object EngineBlobStore {
    @Throws(IOException::class)
    fun copyUriToBlobdir(
        context: Context,
        uri: Uri,
        filename: String,
        ext: String?,
    ): String = EngineBridge.copyToBlobdir(context, uri, filename, ext)
}
