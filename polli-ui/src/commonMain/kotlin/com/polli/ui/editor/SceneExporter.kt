package com.polli.ui.editor

import com.polli.domain.editor.SceneDocument

data class ExportResult(
    /** Platform URI/path string the caller can hand back to the send/attachment flow. */
    val uri: String,
    val mimeType: String,
)

/**
 * Renders a non-destructive [SceneDocument] to a concrete baked file (compressed image or trimmed
 * video). Implemented per platform; Android provides the only implementation today, desktop/iOS can
 * follow. Kept as an injected interface (rather than expect/actual) because the Android renderer
 * needs a `Context` and heavy media dependencies that should not leak into the shared UI module.
 */
interface SceneExporter {
    suspend fun export(doc: SceneDocument): ExportResult
}
