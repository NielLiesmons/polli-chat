package com.polli.engine.rpc

import com.fasterxml.jackson.databind.ObjectMapper
import com.polli.core.chat.ChatCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Offline JSON parsing checks — no live RPC server required. */
class RpcChatListMapperJsonTest {
    private val mapper = ObjectMapper()

    @Test
    fun parsesCamelCaseChatListItemMap() {
        val json =
            """
            {
              "12": {
                "kind": "ChatListItem",
                "id": 12,
                "name": "tekne",
                "chatType": "Single",
                "summaryText1": "hello",
                "summaryText2": "",
                "freshMessageCounter": 2,
                "isDeviceTalk": false,
                "isSelfTalk": false,
                "isGroup": false
              },
              "13": {
                "kind": "ChatListItem",
                "id": 13,
                "name": "MNS Early Adopters",
                "chatType": "Group",
                "summaryText1": "group msg",
                "summaryText2": "",
                "freshMessageCounter": 0,
                "isDeviceTalk": false,
                "isSelfTalk": false,
                "isGroup": true
              }
            }
            """
                .trimIndent()

        val root = mapper.readTree(json)
        val items =
            listOf(12, 13).mapNotNull { id ->
                RpcChatListMapper.parseItemForTest(root.get(id.toString()))
            }

        assertEquals(2, items.size)
        assertEquals(ChatCategory.Mail, items.first { it.chatId == 12 }.category)
        assertEquals(ChatCategory.Space, items.first { it.chatId == 13 }.category)
    }

    @Test
    fun typedJacksonChatListItemDeserializes() {
        val json =
            """
            {
              "12": {
                "kind": "ChatListItem",
                "id": 12,
                "name": "tekne",
                "chatType": "Single",
                "summaryText1": "hello",
                "summaryText2": "",
                "freshMessageCounter": 2,
                "isDeviceTalk": false,
                "isSelfTalk": false,
                "isGroup": false
              }
            }
            """
                .trimIndent()

        val map: Map<String, chat.delta.rpc.types.ChatListItemFetchResult> =
            mapper.readValue(
                json,
                mapper.typeFactory.constructMapType(
                    Map::class.java,
                    String::class.java,
                    chat.delta.rpc.types.ChatListItemFetchResult::class.java,
                ),
            )
        val item = map["12"]
        assertTrue(item is chat.delta.rpc.types.ChatListItemFetchResult.ChatListItem)
    }
}
