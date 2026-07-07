package com.polli.domain.editor

import kotlinx.serialization.json.Json

/**
 * Serialization boundary for [SceneDocument].
 *
 * Swap-ready: [JsonSceneCodec] is the only implementation today, but a compact binary codec
 * (e.g. Postcard, or a Rust-backed codec over FFI) can be dropped in later without changing the
 * model or the editor UI. The interface is byte-oriented so it fits both text and binary formats;
 * JSON additionally exposes string helpers for logging/debugging and drafts.
 */
interface DocumentCodec {
    fun encodeToBytes(doc: SceneDocument): ByteArray

    fun decodeFromBytes(bytes: ByteArray): SceneDocument
}

class JsonSceneCodec(
    private val json: Json = DEFAULT_JSON,
) : DocumentCodec {
    override fun encodeToBytes(doc: SceneDocument): ByteArray = encodeToString(doc).encodeToByteArray()

    override fun decodeFromBytes(bytes: ByteArray): SceneDocument = decodeFromString(bytes.decodeToString())

    fun encodeToString(doc: SceneDocument): String = json.encodeToString(SceneDocument.serializer(), doc)

    fun decodeFromString(data: String): SceneDocument = json.decodeFromString(SceneDocument.serializer(), data)

    companion object {
        val DEFAULT_JSON: Json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            classDiscriminator = "type"
            prettyPrint = false
        }
    }
}
