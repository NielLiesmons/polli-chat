package com.polli.core.chat

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatCategorizerTest {
    @Test
    fun deviceTalkIsSkipped() {
        val kind = ChatKind(
            type = ChatCategorizer.CHAT_TYPE_SINGLE,
            isDeviceTalk = true,
            isSelfTalk = false,
            isOutBroadcast = false,
            isInBroadcast = false,
            isMailingList = false,
            isMultiUser = false,
        )
        assertEquals(ChatCategory.Skip, ChatCategorizer.categorize(kind))
    }

    @Test
    fun groupIsSpace() {
        val kind = ChatKind(
            type = ChatCategorizer.CHAT_TYPE_GROUP,
            isDeviceTalk = false,
            isSelfTalk = false,
            isOutBroadcast = false,
            isInBroadcast = false,
            isMailingList = false,
            isMultiUser = true,
        )
        assertEquals(ChatCategory.Space, ChatCategorizer.categorize(kind))
    }

    @Test
    fun mailingListIsChannel() {
        val kind = ChatKind(
            type = ChatCategorizer.CHAT_TYPE_MAILINGLIST,
            isDeviceTalk = false,
            isSelfTalk = false,
            isOutBroadcast = false,
            isInBroadcast = false,
            isMailingList = true,
            isMultiUser = true,
        )
        assertEquals(ChatCategory.Channel, ChatCategorizer.categorize(kind))
    }
}
