package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException
import chat.delta.rpc.types.ChatListItemFetchResult
import chat.delta.rpc.types.ChatType
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.polli.core.chat.ChatCategorizer
import com.polli.core.chat.ChatCategory
import com.polli.core.chat.ChatKind
import com.polli.core.chat.ChatListFlags
import com.polli.core.chat.ChatSummaryFormat
import com.polli.domain.model.InboxItem

/**
 * Maps JSON-RPC chatlist payloads → [InboxItem].
 *
 * Parsing stays here (transport boundary); tab routing mirrors Rust `polli-home::categorize`.
 */
object RpcChatListMapper {
    fun load(
        rpc: Rpc,
        accountId: Int,
        listFlags: Int,
        query: String? = null,
    ): List<InboxItem> {
        return try {
            val entries =
                rpc.getChatlistEntries(accountId, listFlags, query, null) ?: emptyList()
            if (entries.isEmpty()) {
                log("account=$accountId flags=$listFlags → 0 chatlist entry ids")
                return emptyList()
            }

            val typed = loadTyped(rpc, accountId, entries)
            if (typed.isNotEmpty()) {
                log("account=$accountId typed rows=${typed.size} from ${entries.size} entries")
                return typed
            }

            val json = loadFromRawJson(rpc, accountId, entries)
            log(
                "account=$accountId json rows=${json.size} from ${entries.size} entries " +
                    "(typed path empty)",
            )
            json
        } catch (e: RpcException) {
            log("load failed account=$accountId: ${e.message}")
            emptyList()
        }
    }

    private fun loadTyped(
        rpc: Rpc,
        accountId: Int,
        entries: List<Int>,
    ): List<InboxItem> {
        return try {
            val mapper = rpc.transport.getObjectMapper()
            val rawItems: JsonNode =
                rpc.transport.callForResult(
                    object : TypeReference<JsonNode>() {},
                    "get_chatlist_items_by_entries",
                    mapper.valueToTree(accountId),
                    mapper.valueToTree(entries),
                )
            val byId: Map<String, ChatListItemFetchResult> =
                mapper.readValue(
                    rawItems.traverse(),
                    mapper.typeFactory.constructMapType(
                        Map::class.java,
                        String::class.java,
                        ChatListItemFetchResult::class.java,
                    ),
                )
            entries.mapNotNull { entryId ->
                val result =
                    byId[entryId.toString()]
                        ?: byId.entries.firstOrNull { it.key.toIntOrNull() == entryId }?.value
                val node = rawItems.findEntry(entryId)
                when (result) {
                    is ChatListItemFetchResult.ChatListItem -> toInboxItem(result, node)
                    else -> node?.let(::parseItem)
                }
            }
        } catch (e: RpcException) {
            log("typed deserialize failed: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            log("typed deserialize failed: ${e.message}")
            emptyList()
        }
    }

    private fun loadFromRawJson(
        rpc: Rpc,
        accountId: Int,
        entries: List<Int>,
    ): List<InboxItem> {
        val mapper = rpc.transport.getObjectMapper()
        val rawItems: JsonNode =
            rpc.transport.callForResult(
                object : TypeReference<JsonNode>() {},
                "get_chatlist_items_by_entries",
                mapper.valueToTree(accountId),
                mapper.valueToTree(entries),
            )

        val byChatId = linkedMapOf<Int, InboxItem>()
        when {
            rawItems.isObject -> {
                rawItems.fields().forEachRemaining { (_, node) ->
                    parseItem(node)?.let { byChatId[it.chatId] = it }
                }
            }
            rawItems.isArray -> {
                rawItems.forEach { node -> parseItem(node)?.let { byChatId[it.chatId] = it } }
            }
        }

        if (byChatId.isEmpty()) {
            log(
                "raw json parse produced 0 rows; nodeType=${rawItems.nodeType} " +
                    "keys=${rawItems.fieldNames().asSequence().take(8).toList()} " +
                    "sample=${rawItems.toString().take(400)}",
            )
        }

        return entries.mapNotNull { byChatId[it] ?: rawItems.findEntry(it)?.let(::parseItem) }
    }

    private fun toInboxItem(
        item: ChatListItemFetchResult.ChatListItem,
        raw: JsonNode? = null,
    ): InboxItem? {
        val chatId = item.id ?: return null
        if (chatId <= ChatListFlags.LAST_SPECIAL_CHAT_ID) return null
        val kindMeta = item.toKind()
        val category = ChatCategorizer.categorize(kindMeta)
        if (category == ChatCategory.Skip) return null
        val preview = ChatSummaryFormat.preview(item.summaryText1 ?: "", item.summaryText2 ?: "")
        val name = item.name.orEmpty()
        val updatedAt =
            raw?.readLong("lastUpdated", "last_updated")
                ?: item.lastUpdated?.toLong()
                ?: 0L
        return InboxItem(
            chatId = chatId,
            name = name,
            preview = preview.text,
            previewAuthor = preview.author,
            updatedAt = updatedAt,
            unreadCount = (item.freshMessageCounter ?: 0).coerceAtLeast(0),
            category = category,
            profileImage = item.avatarPath,
            colorSeed = name.ifBlank { chatId.toString() },
        )
    }

    private fun ChatListItemFetchResult.ChatListItem.toKind(): ChatKind {
        val chatType = this.chatType ?: ChatType.Single
        return ChatKind(
            type =
                when (chatType) {
                    ChatType.Group -> ChatCategorizer.CHAT_TYPE_GROUP
                    ChatType.Mailinglist -> ChatCategorizer.CHAT_TYPE_MAILINGLIST
                    ChatType.OutBroadcast -> ChatCategorizer.CHAT_TYPE_OUT_BROADCAST
                    ChatType.InBroadcast -> ChatCategorizer.CHAT_TYPE_IN_BROADCAST
                    ChatType.Single -> ChatCategorizer.CHAT_TYPE_SINGLE
                },
            isDeviceTalk = isDeviceTalk == true,
            isSelfTalk = isSelfTalk == true,
            isOutBroadcast = chatType == ChatType.OutBroadcast,
            isInBroadcast = chatType == ChatType.InBroadcast,
            isMailingList = chatType == ChatType.Mailinglist,
            isMultiUser =
                chatType == ChatType.Group ||
                    chatType == ChatType.Mailinglist ||
                    isGroup == true,
        )
    }

    private fun parseItem(node: JsonNode): InboxItem? {
        if (!node.isChatListItem()) return null
        val chatId = node.readInt("id") ?: return null
        if (chatId <= ChatListFlags.LAST_SPECIAL_CHAT_ID) return null
        val kindMeta = toKind(node)
        val category = ChatCategorizer.categorize(kindMeta)
        if (category == ChatCategory.Skip) return null
        val summaryText1 = node.readText("summaryText1", "summary_text1")
        val summaryText2 = node.readText("summaryText2", "summary_text2")
        val preview = ChatSummaryFormat.preview(summaryText1, summaryText2)
        val name = node.readText("name")
        val seed = name.ifBlank { chatId.toString() }
        return InboxItem(
            chatId = chatId,
            name = name,
            preview = preview.text,
            previewAuthor = preview.author,
            updatedAt = node.readLong("lastUpdated", "last_updated") ?: 0L,
            unreadCount = node.readInt("freshMessageCounter", "fresh_message_counter") ?: 0,
            category = category,
            profileImage = node.readOptionalText("avatarPath", "avatar_path"),
            colorSeed = seed,
        )
    }

    private fun toKind(node: JsonNode): ChatKind {
        val chatType = node.readChatType()
        val isGroup = node.readBoolean("isGroup", "is_group") ?: false
        return ChatKind(
            type =
                when (chatType) {
                    "Group" -> ChatCategorizer.CHAT_TYPE_GROUP
                    "Mailinglist" -> ChatCategorizer.CHAT_TYPE_MAILINGLIST
                    "OutBroadcast" -> ChatCategorizer.CHAT_TYPE_OUT_BROADCAST
                    "InBroadcast" -> ChatCategorizer.CHAT_TYPE_IN_BROADCAST
                    else -> ChatCategorizer.CHAT_TYPE_SINGLE
                },
            isDeviceTalk = node.readBoolean("isDeviceTalk", "is_device_talk") ?: false,
            isSelfTalk = node.readBoolean("isSelfTalk", "is_self_talk") ?: false,
            isOutBroadcast = chatType == "OutBroadcast",
            isInBroadcast = chatType == "InBroadcast",
            isMailingList = chatType == "Mailinglist",
            isMultiUser = chatType == "Group" || chatType == "Mailinglist" || isGroup,
        )
    }

    private fun JsonNode.isChatListItem(): Boolean {
        if (has("error")) return false
        val kind = path("kind").asText("").lowercase()
        return kind == "chatlistitem" || kind == "chat_list_item" || kind.isEmpty()
    }

    private fun JsonNode.findEntry(entryId: Int): JsonNode? {
        get(entryId.toString())?.takeUnless { it.isNull }?.let { return it }
        get(entryId)?.takeUnless { it.isNull }?.let { return it }
        return null
    }

    private fun JsonNode.readChatType(): String {
        val node =
            path("chatType").takeIf { !it.isMissingNode && !it.isNull }
                ?: path("chat_type").takeIf { !it.isMissingNode && !it.isNull }
                ?: return "Single"
        return when {
            node.isTextual -> node.asText()
            node.isObject && node.size() > 0 -> node.fieldNames().next()
            else -> "Single"
        }
    }

    private fun JsonNode.readText(vararg keys: String): String {
        for (key in keys) {
            val node = path(key)
            if (!node.isMissingNode && !node.isNull && node.isTextual) {
                return node.asText()
            }
        }
        return ""
    }

    private fun JsonNode.readOptionalText(vararg keys: String): String? {
        val text = readText(*keys)
        return text.ifBlank { null }
    }

    private fun JsonNode.readInt(vararg keys: String): Int? {
        for (key in keys) {
            val node = path(key)
            if (!node.isMissingNode && !node.isNull && node.isNumber) {
                return node.asInt()
            }
        }
        return null
    }

    private fun JsonNode.readLong(vararg keys: String): Long? {
        for (key in keys) {
            val node = path(key)
            if (!node.isMissingNode && !node.isNull && node.isNumber) {
                return node.asLong()
            }
        }
        return null
    }

    private fun JsonNode.readBoolean(vararg keys: String): Boolean? {
        for (key in keys) {
            val node = path(key)
            if (!node.isMissingNode && !node.isNull && node.isBoolean) {
                return node.asBoolean()
            }
        }
        return null
    }

    private fun log(message: String) {
        println("[RpcChatListMapper] $message")
    }

    /** Test hook for offline JSON fixture parsing. */
    internal fun parseItemForTest(node: com.fasterxml.jackson.databind.JsonNode?): InboxItem? {
        if (node == null || node.isNull) return null
        return parseItem(node)
    }
}
