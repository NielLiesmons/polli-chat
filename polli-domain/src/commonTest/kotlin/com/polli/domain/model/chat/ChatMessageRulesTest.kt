package com.polli.domain.model.chat

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatMessageRulesTest {
    @Test
    fun canReplyToNonInfoMessages() {
        val msg =
            ChatMessage(
                id = 1,
                text = "hi",
                timestamp = 0,
                isOutgoing = false,
                authorId = 2,
                authorName = "A",
                authorColorSeed = "a",
                quote = null,
                hasAttachment = false,
                isInfo = false,
            )
        assertTrue(ChatMessageRules.canReplyToMsg(msg))
    }

    @Test
    fun cannotReplyToInfoMessages() {
        val msg =
            ChatMessage(
                id = 1,
                text = "joined",
                timestamp = 0,
                isOutgoing = false,
                authorId = 0,
                authorName = "",
                authorColorSeed = "",
                quote = null,
                hasAttachment = false,
                isInfo = true,
            )
        assertFalse(ChatMessageRules.canReplyToMsg(msg))
    }
}
