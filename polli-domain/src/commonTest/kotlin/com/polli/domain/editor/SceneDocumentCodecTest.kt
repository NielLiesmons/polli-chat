package com.polli.domain.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SceneDocumentCodecTest {
    private val codec = JsonSceneCodec()

    private fun sampleImageDoc() =
        SceneDocument(
            frame = Frame(1080, 1350),
            layers =
                listOf(
                    MediaLayer(
                        id = "layer-1",
                        source = MediaRef(uri = "content://media/1", mimeType = "image/jpeg"),
                        kind = MediaKind.Image,
                        crop = CropRect(0.1f, 0.2f, 0.9f, 0.8f),
                        transform = Transform(rotationDeg = 90f, flipH = true),
                    ),
                ),
        )

    private fun sampleVideoDoc() =
        SceneDocument(
            frame = Frame(1280, 720),
            layers =
                listOf(
                    MediaLayer(
                        id = "clip-1",
                        source = MediaRef(uri = "content://media/2", mimeType = "video/mp4"),
                        kind = MediaKind.Video,
                        trim = TrimRange(startMs = 1_500L, endMs = 8_000L),
                    ),
                ),
        )

    @Test
    fun imageDocRoundTrips() {
        val doc = sampleImageDoc()
        assertEquals(doc, codec.decodeFromString(codec.encodeToString(doc)))
    }

    @Test
    fun videoDocRoundTripsThroughBytes() {
        val doc = sampleVideoDoc()
        assertEquals(doc, codec.decodeFromBytes(codec.encodeToBytes(doc)))
    }

    @Test
    fun sealedLayerUsesStableTypeDiscriminator() {
        val json = codec.encodeToString(sampleImageDoc())
        assertTrue(json.contains("\"type\":\"media\""), "expected stable 'media' discriminator, got: $json")
    }

    @Test
    fun mixedStubLayersRoundTrip() {
        val doc =
            SceneDocument(
                frame = Frame(1080, 1080),
                layers =
                    listOf(
                        MediaLayer(
                            id = "m",
                            source = MediaRef("content://media/3"),
                            kind = MediaKind.Image,
                        ),
                        TextLayer(id = "t", text = "hi", style = TextStyle(bold = true)),
                        StickerLayer(id = "s", source = MediaRef("content://sticker/1")),
                    ),
            )
        assertEquals(doc, codec.decodeFromString(codec.encodeToString(doc)))
    }

    @Test
    fun unknownKeysAreIgnoredForForwardCompat() {
        val withExtra =
            """{"schemaVersion":1,"frame":{"widthPx":100,"heightPx":100,"futureField":true},"layers":[],"meta":{}}"""
        val decoded = codec.decodeFromString(withExtra)
        assertEquals(Frame(100, 100), decoded.frame)
    }

    @Test
    fun cropAndTrimHelpers() {
        val crop = CropRect(0.25f, 0.25f, 0.75f, 1.0f)
        assertEquals(0.5f, crop.width)
        assertEquals(0.75f, crop.height)
        assertTrue(CropRect.FULL.isFull)
        assertEquals(6_500L, TrimRange(1_500L, 8_000L).durationMs)
    }
}
