package com.polli.engine.rpc

import chat.delta.rpc.types.Message
import chat.delta.rpc.types.Viewtype
import com.polli.domain.model.chat.OutgoingState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RpcMessageMapperTest {
    // Regression: DC_STATE_IN_SEEN == 16 must NOT be treated as outgoing.
    @Test
    fun seenIncomingMessageIsNotOutgoing() {
        val msg = Message()
        msg.id = 1
        msg.fromId = 7
        msg.state = 16 // DC_STATE_IN_SEEN
        msg.overrideSenderName = "Alice"
        msg.text = "hi"

        val chat = RpcMessageMapper.toChatMessage(msg, selfName = "Me")!!
        assertFalse(chat.isOutgoing)
        assertEquals("Alice", chat.authorName)
        assertNull(chat.outgoingState)

        val stub = RpcMessageMapper.toStub(msg, selfName = "Me")!!
        assertFalse(stub.isOutgoing)
    }

    @Test
    fun selfContactMessageIsOutgoing() {
        val msg = Message()
        msg.id = 2
        msg.fromId = 1 // DC_CONTACT_ID_SELF
        msg.state = 26 // DC_STATE_OUT_DELIVERED
        msg.text = "yo"

        val chat = RpcMessageMapper.toChatMessage(msg, selfName = "Me")!!
        assertTrue(chat.isOutgoing)
        assertEquals("Me", chat.authorName)
        assertEquals(OutgoingState.Sent, chat.outgoingState)
    }

    @Test
    fun outgoingStateMapping() {
        fun stateFor(state: Int): OutgoingState? {
            val msg = Message().apply { id = 9; fromId = 1; this.state = state; text = "x" }
            return RpcMessageMapper.toChatMessage(msg)!!.outgoingState
        }
        assertEquals(OutgoingState.Sending, stateFor(20)) // OUT_PENDING
        assertEquals(OutgoingState.Sending, stateFor(18)) // OUT_PREPARING
        assertEquals(OutgoingState.Failed, stateFor(24)) // OUT_FAILED
        assertEquals(OutgoingState.Sent, stateFor(26)) // OUT_DELIVERED
        assertEquals(OutgoingState.Read, stateFor(28)) // OUT_MDN_RCVD
    }

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
