package com.polli.domain.editor

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Editable, non-destructive media document for the editor foundation.
 *
 * The document only ever stores *parameters* (crop, trim, transforms, filters) over source-media
 * references; rendering/export "bakes" them. This keeps every edit re-editable and lets one document
 * render to many targets (compressed image, trimmed video, later HTML/Nostr for Stories).
 *
 * The model carries no Compose/Android types on purpose so it stays portable and so the serialization
 * format can be swapped (see [DocumentCodec]) without touching the model or UI.
 */
const val SCENE_SCHEMA_VERSION: Int = 1

@Serializable
data class SceneDocument(
    val schemaVersion: Int = SCENE_SCHEMA_VERSION,
    val frame: Frame,
    val layers: List<Layer> = emptyList(),
    val meta: Map<String, String> = emptyMap(),
)

@Serializable
data class Frame(
    val widthPx: Int,
    val heightPx: Int,
) {
    val aspect: Float get() = if (heightPx == 0) 1f else widthPx.toFloat() / heightPx.toFloat()
}

@Serializable
sealed class Layer {
    abstract val id: String
    abstract val transform: Transform
    abstract val meta: Map<String, String>
}

@Serializable
@SerialName("media")
data class MediaLayer(
    override val id: String,
    val source: MediaRef,
    val kind: MediaKind,
    override val transform: Transform = Transform(),
    /** Normalized crop over the source; null means "use full source". */
    val crop: CropRect? = null,
    /** Video-only trim window; null means "whole clip". */
    val trim: TrimRange? = null,
    val filters: List<Filter> = emptyList(),
    override val meta: Map<String, String> = emptyMap(),
) : Layer()

/** Declared for Stories/annotations; not edited by the MVP editor yet. */
@Serializable
@SerialName("text")
data class TextLayer(
    override val id: String,
    val text: String,
    val style: TextStyle = TextStyle(),
    override val transform: Transform = Transform(),
    override val meta: Map<String, String> = emptyMap(),
) : Layer()

/** Declared for Stories/annotations; not edited by the MVP editor yet. */
@Serializable
@SerialName("sticker")
data class StickerLayer(
    override val id: String,
    val source: MediaRef,
    override val transform: Transform = Transform(),
    override val meta: Map<String, String> = emptyMap(),
) : Layer()

@Serializable
enum class MediaKind { Image, Video }

@Serializable
data class MediaRef(
    val uri: String,
    val mimeType: String? = null,
)

@Serializable
data class Transform(
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
)

/** Normalized crop rectangle, each edge in `0..1` relative to the (rotation-applied) source. */
@Serializable
data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val isFull: Boolean get() = left <= 0f && top <= 0f && right >= 1f && bottom >= 1f

    companion object {
        val FULL: CropRect = CropRect()
    }
}

@Serializable
data class TrimRange(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

/** Open-ended filter descriptor; unused by the MVP but part of the swap-ready schema. */
@Serializable
data class Filter(
    val type: String,
    val params: Map<String, Float> = emptyMap(),
)

@Serializable
data class TextStyle(
    val colorArgb: Int = -0x1, // opaque white
    val sizeSp: Float = 24f,
    val bold: Boolean = false,
    val align: String = "center",
)
