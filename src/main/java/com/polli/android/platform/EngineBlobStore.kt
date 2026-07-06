package com.polli.android.platform

import android.content.Context
import android.net.Uri
import org.thoughtcrime.securesms.connect.DcHelper
import java.io.IOException

/** Copies content URIs into the engine blob directory (thin DcHelper wrapper). */
object EngineBlobStore {
    @Throws(IOException::class)
    fun copyUriToBlobdir(
        context: Context,
        uri: Uri,
        filename: String,
        ext: String?,
    ): String = DcHelper.copyToBlobdir(context, uri, filename, ext)
}
