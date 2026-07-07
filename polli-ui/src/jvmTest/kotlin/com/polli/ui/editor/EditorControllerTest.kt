package com.polli.ui.editor

import com.polli.domain.editor.CropRect
import com.polli.domain.editor.Frame
import com.polli.domain.editor.MediaKind
import com.polli.domain.editor.MediaLayer
import com.polli.domain.editor.MediaRef
import com.polli.domain.editor.SceneDocument
import com.polli.domain.editor.TrimRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorControllerTest {
    private fun imageController(): EditorController =
        EditorController(
            SceneDocument(
                frame = Frame(1000, 1000),
                layers =
                    listOf(
                        MediaLayer(
                            id = "m",
                            source = MediaRef("content://x", "image/jpeg"),
                            kind = MediaKind.Image,
                        ),
                    ),
            ),
        )

    @Test
    fun selectsPrimaryMediaByDefault() {
        val c = imageController()
        assertEquals("m", c.selectedLayerId)
        assertEquals("m", c.primaryMedia?.id)
        assertFalse(c.canUndo)
        assertFalse(c.canRedo)
    }

    @Test
    fun setCropRecordsHistory() {
        val c = imageController()
        c.setCrop("m", CropRect(0.1f, 0.1f, 0.9f, 0.9f))
        assertEquals(CropRect(0.1f, 0.1f, 0.9f, 0.9f), c.primaryMedia?.crop)
        assertTrue(c.canUndo)
    }

    @Test
    fun fullCropIsStoredAsNull() {
        val c = imageController()
        c.setCrop("m", CropRect(0.2f, 0.2f, 0.8f, 0.8f))
        c.setCrop("m", CropRect.FULL)
        assertNull(c.primaryMedia?.crop)
    }

    @Test
    fun rotateWrapsAtThreeSixty() {
        val c = imageController()
        repeat(5) { c.rotate90("m") }
        assertEquals(90f, c.primaryMedia?.transform?.rotationDeg)
    }

    @Test
    fun undoRedoRestoresDocument() {
        val c = imageController()
        c.setCrop("m", CropRect(0.1f, 0.1f, 0.9f, 0.9f))
        c.rotate90("m")
        val afterTwo = c.document

        c.undo()
        assertEquals(0f, c.primaryMedia?.transform?.rotationDeg)
        c.undo()
        assertNull(c.primaryMedia?.crop)
        assertFalse(c.canUndo)

        c.redo()
        c.redo()
        assertEquals(afterTwo, c.document)
        assertFalse(c.canRedo)
    }

    @Test
    fun newEditClearsRedo() {
        val c = imageController()
        c.setCrop("m", CropRect(0.1f, 0.1f, 0.9f, 0.9f))
        c.undo()
        assertTrue(c.canRedo)
        c.rotate90("m")
        assertFalse(c.canRedo)
    }

    @Test
    fun setTrimUpdatesVideoLayer() {
        val c =
            EditorController(
                SceneDocument(
                    frame = Frame(1280, 720),
                    layers =
                        listOf(
                            MediaLayer(
                                id = "v",
                                source = MediaRef("content://v", "video/mp4"),
                                kind = MediaKind.Video,
                            ),
                        ),
                ),
            )
        c.setTrim("v", TrimRange(1000L, 5000L))
        assertEquals(4000L, c.primaryMedia?.trim?.durationMs)
    }

    @Test
    fun noopUpdateDoesNotRecordHistory() {
        val c = imageController()
        c.setCrop("m", CropRect.FULL)
        assertFalse(c.canUndo)
    }
}
