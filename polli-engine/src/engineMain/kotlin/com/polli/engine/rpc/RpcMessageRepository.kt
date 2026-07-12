package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException
import chat.delta.rpc.types.Message
import chat.delta.rpc.types.MessageData
import chat.delta.rpc.types.MessageListItem
import chat.delta.rpc.types.MessageLoadResult
import chat.delta.rpc.types.Viewtype
import com.polli.domain.model.chat.ChatListItem
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.MessageStub
import com.polli.domain.repository.MessageRepository

class RpcMessageRepository(
    private val rpc: Rpc,
    private val accountId: Int,
    private val eventLoop: RpcEventLoop,
) : MessageRepository {
    private val messageCache =
        object : LinkedHashMap<Int, ChatMessage>(MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, ChatMessage>?): Boolean =
                size > MAX_CACHE_SIZE
        }

    private val stubCache =
        object : LinkedHashMap<Int, MessageStub>(MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, MessageStub>?): Boolean =
                size > MAX_CACHE_SIZE
        }

    private val reactionCache =
        object : LinkedHashMap<Int, List<com.polli.domain.model.chat.MessageReaction>>(MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<com.polli.domain.model.chat.MessageReaction>>?): Boolean =
                size > MAX_CACHE_SIZE
        }

    override fun getMessageIds(chatId: Int, addDaymarker: Boolean): IntArray {
        return try {
            (rpc.getMessageIds(accountId, chatId, false, addDaymarker) ?: emptyList())
                .map { it.toInt() }
                .toIntArray()
        } catch (_: RpcException) {
            intArrayOf()
        }
    }

    override fun getMessageListItems(chatId: Int, addDaymarker: Boolean): List<ChatListItem>? {
        return try {
            rpc.getMessageListItems(accountId, chatId, false, addDaymarker)
                ?.mapNotNull { item ->
                    when (item) {
                        is MessageListItem.Message ->
                            item.msg_id?.let { ChatListItem.Message(it) }
                        is MessageListItem.DayMarker ->
                            item.timestamp?.toLong()?.let { ChatListItem.DayMarker(it) }
                        else -> null
                    }
                }
        } catch (_: RpcException) {
            null
        }
    }

    /** Never RPC on cache miss — callers must preload stubs first (DC LRU warm-up). */
    override fun getStub(msgId: Int): MessageStub? {
        stubCache[msgId]?.let { return it }
        messageCache[msgId]?.let { return stubFromMessage(it) }
        return null
    }

    override fun preloadMessages(msgIds: IntArray) {
        if (msgIds.isEmpty()) return
        val missing =
            msgIds.filter { id ->
                id > 0 && !stubCache.containsKey(id) && !messageCache.containsKey(id)
            }
        if (missing.isEmpty()) return
        val chunks = missing.chunked(BATCH_SIZE)
        for (chunk in chunks) {
            try {
                val batch = rpc.getMessages(accountId, chunk) ?: continue
                for ((key, result) in batch) {
                    val msgId = key.toIntOrNull() ?: continue
                    if (result is MessageLoadResult.Message) {
                        cacheFromLoadResult(msgId, result)
                    }
                }
            } catch (_: RpcException) {
                // skip failed chunk
            }
        }
    }

    override fun getMessage(msgId: Int): ChatMessage? {
        messageCache[msgId]?.let { return it }
        return loadMessage(msgId)
    }

    private fun loadMessage(msgId: Int): ChatMessage? {
        return try {
            val msg = rpc.getMessage(accountId, msgId) ?: return null
            RpcMessageMapper.toChatMessage(msg)?.also { chatMsg ->
                messageCache[msgId] = chatMsg
                RpcMessageMapper.toStub(msg)?.let { stubCache[msgId] = it }
            }
        } catch (_: RpcException) {
            null
        }
    }

    private fun stubFromMessage(message: ChatMessage): MessageStub =
        MessageStub(
            id = message.id,
            timestamp = message.timestamp,
            isOutgoing = message.isOutgoing,
            authorId = message.authorId,
            authorName = message.authorName,
            authorColorSeed = message.authorColorSeed,
            isEdited = message.isEdited,
            isInfo = message.isInfo,
            hasText = message.text.isNotEmpty(),
            hasAttachment = message.hasAttachment,
        )

    override fun getDraft(chatId: Int): String {
        return try {
            rpc.getDraft(accountId, chatId)?.text?.orEmpty() ?: ""
        } catch (_: RpcException) {
            ""
        }
    }

    override fun setDraft(chatId: Int, text: String, quotedMessageId: Int?) {
        try {
            if (text.isBlank() && quotedMessageId == null) {
                rpc.removeDraft(accountId, chatId)
                return
            }
            rpc.miscSetDraft(
                accountId,
                chatId,
                text,
                null,
                null,
                quotedMessageId,
                Viewtype.Text,
            )
        } catch (_: RpcException) {
            // ignore
        }
    }

    override fun clearDraft(chatId: Int) {
        try {
            rpc.removeDraft(accountId, chatId)
        } catch (_: RpcException) {
            // ignore
        }
    }

    override fun sendDraft(chatId: Int): Int? {
        return try {
            rpc.miscSendDraft(accountId, chatId)
        } catch (_: RpcException) {
            null
        }
    }

    override fun deleteMessages(msgIds: IntArray) {
        if (msgIds.isEmpty()) return
        try {
            rpc.deleteMessages(accountId, msgIds.toList())
            for (id in msgIds) {
                messageCache.remove(id)
                stubCache.remove(id)
            }
        } catch (_: RpcException) {
            // ignore
        }
    }

    override fun marknoticedChat(chatId: Int) {
        try {
            rpc.marknoticedChat(accountId, chatId)
        } catch (_: RpcException) {
            // ignore
        }
    }

    override fun getFreshMessageCount(chatId: Int): Int {
        return try {
            rpc.getFreshMsgCnt(accountId, chatId)?.coerceAtLeast(0) ?: 0
        } catch (_: RpcException) {
            0
        }
    }

    override fun sendReaction(msgId: Int, emoji: String) {
        try {
            rpc.sendReaction(accountId, msgId, listOf(emoji))
        } catch (_: RpcException) {
            // ignore
        }
    }

    override fun getMessageReactions(msgId: Int): List<com.polli.domain.model.chat.MessageReaction> {
        reactionCache[msgId]?.let { return it }
        return loadMessageReactions(msgId).also { reactionCache[msgId] = it }
    }

    private fun loadMessageReactions(msgId: Int): List<com.polli.domain.model.chat.MessageReaction> {
        return try {
            val summary = rpc.getMessageReactions(accountId, msgId) ?: return emptyList()
            val byEmoji = linkedMapOf<String, MutableList<Int>>()
            summary.reactionsByContact?.forEach { (contactKey, emojis) ->
                val contactId = contactKey.toIntOrNull() ?: return@forEach
                emojis?.forEach { emoji ->
                    if (!emoji.isNullOrBlank()) {
                        byEmoji.getOrPut(emoji) { mutableListOf() }.add(contactId)
                    }
                }
            }
            summary.reactions
                ?.mapNotNull { reaction ->
                    val emoji = reaction.emoji ?: return@mapNotNull null
                    val count = reaction.count ?: 1
                    val contactIds = byEmoji[emoji].orEmpty()
                    val reactors =
                        contactIds.take(3).map { contactId ->
                            reactorFor(contactId)
                        }
                    com.polli.domain.model.chat.MessageReaction(
                        emoji = emoji,
                        count = count,
                        reactors = reactors,
                    )
                }
                ?: emptyList()
        } catch (_: RpcException) {
            emptyList()
        }
    }

    override fun sendMedia(
        chatId: Int,
        filePath: String,
        fileName: String?,
        mimeType: String?,
        caption: String?,
        viewType: String,
        quotedMessageId: Int?,
    ): Int? {
        return try {
            val blobPath = rpc.copyToBlobDir(accountId, filePath) ?: filePath
            val data = MessageData()
            data.file = blobPath
            data.filename = fileName
            data.text = caption?.trim().orEmpty().ifBlank { null }
            data.viewtype = RpcMessageMapper.viewtypeFromName(viewType)
            if (quotedMessageId != null && quotedMessageId > 0) {
                data.quotedMessageId = quotedMessageId
            }
            rpc.sendMsg(accountId, chatId, data)
        } catch (_: RpcException) {
            null
        }
    }

    override fun editMessage(msgId: Int, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return
        try {
            rpc.sendEditRequest(accountId, msgId, trimmed)
            messageCache.remove(msgId)
            stubCache.remove(msgId)
        } catch (_: RpcException) {
            // ignore
        }
    }

    override fun resendMessages(msgIds: IntArray) {
        if (msgIds.isEmpty()) return
        try {
            rpc.resendMessages(accountId, msgIds.toList())
        } catch (_: RpcException) {
            // ignore
        }
    }

    override fun getMessageInfo(msgId: Int): String? {
        return try {
            rpc.getMessageInfo(accountId, msgId)
        } catch (_: RpcException) {
            null
        }
    }

    override fun saveMessage(msgId: Int) {
        try {
            rpc.saveMsgs(accountId, listOf(msgId))
        } catch (_: RpcException) {
            // ignore
        }
    }

    override fun unsaveMessage(savedMsgId: Int) {
        if (savedMsgId <= 0) return
        try {
            rpc.deleteMessages(accountId, listOf(savedMsgId))
        } catch (_: RpcException) {
            // ignore
        }
    }

    override fun copyToBlobDir(localPath: String): String? {
        return try {
            rpc.copyToBlobDir(accountId, localPath)
        } catch (_: RpcException) {
            null
        }
    }

    override fun observeEngineEvents(onUpdate: () -> Unit): AutoCloseable =
        eventLoop.addListener(onUpdate)

    override fun clearMessageCaches() {
        messageCache.clear()
        stubCache.clear()
        reactionCache.clear()
    }

    private fun cacheFromLoadResult(msgId: Int, loaded: MessageLoadResult.Message) {
        val rpcMsg = loaded.toRpcMessage(msgId)
        RpcMessageMapper.toStub(rpcMsg)?.let { stubCache[msgId] = it }
        RpcMessageMapper.toChatMessage(rpcMsg)?.let { messageCache[msgId] = it }
    }

    private fun MessageLoadResult.Message.toRpcMessage(msgId: Int): Message {
        val msg = Message()
        msg.id = id ?: msgId
        msg.chatId = chatId
        msg.text = text
        msg.timestamp = timestamp
        msg.fromId = fromId
        msg.isEdited = isEdited
        msg.isInfo = isInfo
        msg.file = file
        msg.fileName = fileName
        msg.viewType = viewType
        msg.state = state
        msg.dimensionsWidth = dimensionsWidth
        msg.dimensionsHeight = dimensionsHeight
        msg.duration = duration
        msg.savedMessageId = savedMessageId
        msg.overrideSenderName = overrideSenderName
        msg.sender = sender
        msg.quote = quote
        return msg
    }

    private fun reactorFor(contactId: Int): com.polli.domain.model.chat.ReactionReactor {
        val name =
            try {
                if (contactId == 1) {
                    "You"
                } else {
                    val contact = rpc.getContact(accountId, contactId)
                    contact.displayName?.takeIf { it.isNotBlank() } ?: "Unknown"
                }
            } catch (_: RpcException) {
                "Unknown"
            }
        return com.polli.domain.model.chat.ReactionReactor(contactId = contactId, name = name)
    }

    private companion object {
        const val MAX_CACHE_SIZE = 40
        const val BATCH_SIZE = 32
    }
}
