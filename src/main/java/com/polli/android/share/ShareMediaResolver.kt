package com.polli.android.share

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.polli.android.platform.PlatformMedia
import de.cketti.safecontentresolver.SafeContentResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Copies inbound share URIs into Polli blob storage for relay to chat. */
object ShareMediaResolver {
    suspend fun resolve(context: Context, uri: Uri): Uri? =
        withContext(Dispatchers.IO) {
            if (PlatformMedia.isLocalAttachmentUri(uri)) return@withContext uri
            try {
                val resolver = SafeContentResolver.newInstance(context)
                val inputStream = resolver.openInputStream(uri) ?: return@withContext null
                inputStream.use { stream ->
                    var fileName: String? = null
                    var fileSize: Long? = null
                    val cursor: Cursor? =
                        context.contentResolver.query(
                            uri,
                            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                            null,
                            null,
                            null,
                        )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                            fileSize = it.getLong(it.getColumnIndexOrThrow(OpenableColumns.SIZE))
                        }
                    }
                    if (fileName == null) fileName = uri.lastPathSegment
                    val mimeType = PlatformMedia.mimeType(context, uri)
                    PlatformMedia.createPersistentBlobFromStream(
                        context,
                        stream,
                        mimeType,
                        fileName,
                        fileSize,
                    )
                }
            } catch (_: Exception) {
                null
            }
        }
}
