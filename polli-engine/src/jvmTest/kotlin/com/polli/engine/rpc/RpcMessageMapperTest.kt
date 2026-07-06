package com.polli.engine.rpc

import chat.delta.rpc.types.Message
import chat.delta.rpc.types.Viewtype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RpcMessageMapperTest {
    @Test
    fun mapsVoiceMessageWithMediaFields() {
        val msg = Message()
        msg.id = 42
        msg.fromId = 7
        msg.text = ""
        msg.timestamp = 1_700_000_000
        msg.state = 17
        msg.viewType = Viewtype.Voice
        msg.file = "/blob/voice.m4a"
        msg.fileName = "voice.m4a"
        msg.dimensionsWidth = 0
        msg.dimensionsHeight = 0
        msg.duration = 12_000
        msg.isInfo = false
        msg.isEdited = false

        val chat = RpcMessageMapper.toChatMessage(msg, selfName = "Me")
        assertNotNull(chat)
        chat!!
        assertEquals(42, chat.id)
        assertEquals("Voice", chat.viewType)
        assertEquals("/blob/voice.m4a", chat.filePath)
        assertEquals(12_000, chat.durationMs)
        assertTrue(chat.hasAttachment)
    }

    @Test
    fun viewtypeRoundTrip() {
        assertEquals(Viewtype.Gif, RpcMessageMapper.viewtypeFromName("Gif"))
        assertEquals(Viewtype.Voice, RpcMessageMapper.viewtypeFromName("Voice"))
        assertEquals(Viewtype.Text, RpcMessageMapper.viewtypeFromName("Unknown"))
    }
}
