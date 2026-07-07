package com.polli.android.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.polli.android.BaseComposeActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliTheme
import com.polli.domain.editor.Frame
import com.polli.domain.editor.MediaKind
import com.polli.domain.editor.MediaLayer
import com.polli.domain.editor.MediaRef
import com.polli.domain.editor.SceneDocument
import com.polli.ui.editor.CropPreset
import com.polli.ui.editor.EditorController
import com.polli.ui.editor.MediaEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Compose host for the shared [MediaEditor]. Decodes the source into a preview bitmap (EXIF-corrected
 * for images, a representative frame for videos), drives the shared editor, and on Done bakes the
 * non-destructive [SceneDocument] via [AndroidSceneExporter], returning the result URI as the intent
 * data. Replaces the legacy Java scribble editor for the crop/trim flow.
 */
class MediaEditorActivity : BaseComposeActivity() {
    enum class Mode { IMAGE, VIDEO, AVATAR }

    private var preview by mutableStateOf<ImageBitmap?>(null)
    private var durationMs by mutableStateOf(0L)
    private var exporting by mutableStateOf(false)

    private lateinit var mode: Mode
    private lateinit var source: Uri
    private var controller: EditorController? = null

    private val exporter by lazy { AndroidSceneExporter(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        source = intent.data ?: intent.getParcelableExtra(EXTRA_SOURCE) ?: run {
            finish()
            return
        }
        mode = runCatching { Mode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: Mode.IMAGE.name) }
            .getOrDefault(Mode.IMAGE)

        loadPreview()

        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                val bmp = preview
                if (bmp != null) {
                    val ctrl = remember(bmp) { buildController(bmp) }.also { controller = it }
                    MediaEditor(
                        controller = ctrl,
                        preview = bmp,
                        isVideo = mode == Mode.VIDEO,
                        durationMs = durationMs,
                        exporting = exporting,
                        accent = MaterialTheme.colorScheme.primary,
                        presets = if (mode == Mode.AVATAR) listOf(CropPreset.Square) else CropPreset.entries,
                        initialPreset = if (mode == Mode.AVATAR) CropPreset.Square else CropPreset.Free,
                        onCancel = { setResult(RESULT_CANCELED); finish() },
                        onDone = { export(ctrl) },
                    )
                }
            }
        }
    }

    private fun buildController(bmp: ImageBitmap): EditorController {
        val doc =
            SceneDocument(
                frame = Frame(bmp.width, bmp.height),
                layers =
                    listOf(
                        MediaLayer(
                            id = "primary",
                            source = MediaRef(source.toString(), contentResolver.getType(source)),
                            kind = if (mode == Mode.VIDEO) MediaKind.Video else MediaKind.Image,
                        ),
                    ),
            )
        return EditorController(doc)
    }

    private fun loadPreview() {
        lifecycleScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    if (mode == Mode.VIDEO) decodeVideoFrame(source) else decodeImage(source)
                }
            preview = result?.first?.asImageBitmap()
            durationMs = result?.second ?: 0L
            if (preview == null) {
                Toast.makeText(this@MediaEditorActivity, "Could not open media", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun export(ctrl: EditorController) {
        if (exporting) return
        exporting = true
        lifecycleScope.launch {
            try {
                val result = exporter.export(ctrl.document)
                val data = Intent().setData(Uri.parse(result.uri))
                setResult(RESULT_OK, data)
                finish()
            } catch (e: Exception) {
                exporting = false
                Toast.makeText(this@MediaEditorActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun decodeImage(uri: Uri): Pair<Bitmap, Long>? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > PREVIEW_MAX || bounds.outHeight / sample > PREVIEW_MAX) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null
        val oriented = applyExif(uri, raw)
        return oriented to 0L
    }

    private fun applyExif(uri: Uri, bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        contentResolver.openInputStream(uri)?.use { input ->
            val exif = ExifInterface(input)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f); matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f); matrix.postScale(-1f, 1f)
                }
            }
        }
        return if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun decodeVideoFrame(uri: Uri): Pair<Bitmap, Long>? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val duration =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            frame to duration
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    companion object {
        private const val EXTRA_SOURCE = "com.polli.android.media.SOURCE"
        private const val EXTRA_MODE = "com.polli.android.media.MODE"
        private const val PREVIEW_MAX = 2048

        fun intent(context: Context, source: Uri, mode: Mode): Intent =
            Intent(context, MediaEditorActivity::class.java).apply {
                setData(source)
                putExtra(EXTRA_MODE, mode.name)
                putExtra(EXTRA_SOURCE, source)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
    }
}
