package com.polli.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.polli.domain.editor.CropRect
import com.polli.domain.editor.MediaLayer
import com.polli.domain.editor.SceneDocument
import com.polli.domain.editor.TrimRange

enum class EditorTool { None, Crop, Trim }

/**
 * Editing state for a single [SceneDocument]: current document, selection, active tool, and a
 * snapshot-based undo/redo stack. Pure state (no platform/IO) so it is shared across Android and
 * desktop and unit-testable on the JVM.
 */
class EditorController(initial: SceneDocument) {
    var document by mutableStateOf(initial)
        private set

    var selectedLayerId by mutableStateOf(initial.layers.firstOrNull()?.id)
        private set

    var activeTool by mutableStateOf(EditorTool.None)

    var canUndo by mutableStateOf(false)
        private set

    var canRedo by mutableStateOf(false)
        private set

    private val undoStack = ArrayDeque<SceneDocument>()
    private val redoStack = ArrayDeque<SceneDocument>()

    /** The layer the MVP editor operates on (single-media editing). */
    val primaryMedia: MediaLayer?
        get() = document.layers.firstOrNull { it is MediaLayer } as? MediaLayer

    fun select(id: String?) {
        selectedLayerId = id
    }

    fun updateMediaLayer(id: String, block: (MediaLayer) -> MediaLayer) {
        val idx = document.layers.indexOfFirst { it.id == id }
        if (idx < 0) return
        val layer = document.layers[idx] as? MediaLayer ?: return
        val updated = block(layer)
        if (updated == layer) return
        val newLayers = document.layers.toMutableList().also { it[idx] = updated }
        commit(document.copy(layers = newLayers))
    }

    fun setCrop(id: String, crop: CropRect) =
        updateMediaLayer(id) { it.copy(crop = crop.takeUnless { c -> c.isFull }) }

    fun setTrim(id: String, trim: TrimRange) = updateMediaLayer(id) { it.copy(trim = trim) }

    fun rotate90(id: String) =
        updateMediaLayer(id) {
            it.copy(transform = it.transform.copy(rotationDeg = normalizeDeg(it.transform.rotationDeg + 90f)))
        }

    fun toggleFlipH(id: String) =
        updateMediaLayer(id) { it.copy(transform = it.transform.copy(flipH = !it.transform.flipH)) }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(document)
        document = prev
        refreshFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(document)
        document = next
        refreshFlags()
    }

    private fun commit(next: SceneDocument) {
        if (next == document) return
        undoStack.addLast(document)
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        document = next
        refreshFlags()
    }

    private fun refreshFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    companion object {
        const val MAX_HISTORY: Int = 50

        fun normalizeDeg(deg: Float): Float {
            var r = deg % 360f
            if (r < 0f) r += 360f
            return r
        }
    }
}
