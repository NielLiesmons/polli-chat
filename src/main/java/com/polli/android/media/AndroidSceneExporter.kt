package com.polli.android.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.Effects
import androidx.media3.transformer.Transformer
import com.polli.android.platform.PlatformLegacyUtil
import com.polli.domain.editor.CropRect
import com.polli.domain.editor.MediaKind
import com.polli.domain.editor.MediaLayer
import com.polli.domain.editor.SceneDocument
import com.polli.ui.editor.ExportResult
import com.polli.ui.editor.SceneExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bakes a [SceneDocument] into a concrete file on Android. Images are decoded, EXIF-corrected,
 * rotated/cropped and compressed to WebP; videos are trimmed/cropped via Media3 [Transformer].
 * The result is returned as a FileProvider URI the chat send flow can consume like any attachment.
 */
@UnstableApi
class AndroidSceneExporter(
    private val context: Context,
    private val imageQuality: Int = 90,
    private val maxImageDimension: Int = 4096,
) : SceneExporter {
    override suspend fun export(doc: SceneDocument): ExportResult {
        val media = doc.layers.firstOrNull { it is MediaLayer } as? MediaLayer
            ?: error("SceneDocument has no media layer to export")
        return when (media.kind) {
            MediaKind.Image -> exportImage(media)
            MediaKind.Video -> exportVideo(media)
        }
    }

    private suspend fun exportImage(layer: MediaLayer): ExportResult = withContext(Dispatchers.Default) {
        val source = Uri.parse(layer.source.uri)
        val base = decodeBounded(source) ?: error("Could not decode image: $source")
        val corrected = base.applyMatrix(exifMatrix(source))
        val userMatrix = Matrix().apply {
            if (layer.transform.flipH) postScale(-1f, 1f)
            val deg = layer.transform.rotationDeg
            if (deg != 0f) postRotate(deg)
        }
        val oriented = corrected.applyMatrix(userMatrix)
        val cropped = oriented.applyCrop(layer.crop)

        val useLossy = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        @Suppress("DEPRECATION")
        val format = if (useLossy) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        val outFile = cacheFile("webp")
        FileOutputStream(outFile).use { cropped.compress(format, imageQuality, it) }

        ExportResult(uri = fileUri(outFile), mimeType = "image/webp")
    }

    private suspend fun exportVideo(layer: MediaLayer): ExportResult = withContext(Dispatchers.Main) {
        val outFile = cacheFile("mp4")
        suspendCancellableCoroutine { cont ->
            val itemBuilder = MediaItem.Builder().setUri(Uri.parse(layer.source.uri))
            layer.trim?.let { trim ->
                if (trim.durationMs > 0) {
                    itemBuilder.setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(trim.startMs)
                            .setEndPositionMs(trim.endMs)
                            .build(),
                    )
                }
            }

            val videoEffects = buildList<Effect> {
                val deg = layer.transform.rotationDeg
                if (deg != 0f) {
                    // Media3 rotates counter-clockwise; our model is clockwise.
                    add(ScaleAndRotateTransformation.Builder().setRotationDegrees(-deg).build())
                }
                layer.crop?.let { c ->
                    if (!c.isFull) add(c.toMedia3Crop())
                }
            }

            val edited = EditedMediaItem.Builder(itemBuilder.build())
                .setEffects(Effects(emptyList(), videoEffects))
                .build()

            val transformer = Transformer.Builder(context)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, result: androidx.media3.transformer.ExportResult) {
                            cont.resume(ExportResult(uri = fileUri(outFile), mimeType = "video/mp4"))
                        }

                        override fun onError(
                            composition: Composition,
                            result: androidx.media3.transformer.ExportResult,
                            exception: ExportException,
                        ) {
                            cont.resumeWithException(exception)
                        }
                    },
                )
                .build()

            cont.invokeOnCancellation { transformer.cancel() }
            transformer.start(edited, outFile.absolutePath)
        }
    }

    private fun decodeBounded(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxImageDimension || bounds.outHeight / sample > maxImageDimension) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun exifMatrix(uri: Uri): Matrix {
        val matrix = Matrix()
        context.contentResolver.openInputStream(uri)?.use { input ->
            val exif = ExifInterface(input)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
            }
        }
        return matrix
    }

    private fun Bitmap.applyMatrix(matrix: Matrix): Bitmap =
        if (matrix.isIdentity) this else Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)

    private fun Bitmap.applyCrop(crop: CropRect?): Bitmap {
        if (crop == null || crop.isFull) return this
        val left = (crop.left * width).toInt().coerceIn(0, width - 1)
        val top = (crop.top * height).toInt().coerceIn(0, height - 1)
        val right = (crop.right * width).toInt().coerceIn(left + 1, width)
        val bottom = (crop.bottom * height).toInt().coerceIn(top + 1, height)
        return Bitmap.createBitmap(this, left, top, right - left, bottom - top)
    }

    private fun CropRect.toMedia3Crop(): Crop =
        Crop(left * 2f - 1f, right * 2f - 1f, 1f - bottom * 2f, 1f - top * 2f)

    private fun cacheFile(extension: String): File {
        val dir = File(context.cacheDir, "editor").apply { mkdirs() }
        return File(dir, "polli_edit_${System.currentTimeMillis()}.$extension")
    }

    private fun fileUri(file: File): String =
        PlatformLegacyUtil.fileProviderUri(context, file).toString()
}
