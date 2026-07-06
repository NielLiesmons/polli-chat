package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException
import chat.delta.rpc.types.MessageData
import chat.delta.rpc.types.Viewtype
import com.polli.core.chat.ChatMediaFilter
import com.polli.core.chat.MsgTypes
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.MessageStub
import com.polli.domain.repository.MessageRepository

class RpcMessageRepository(
    private val rpc: Rpc,
    private val accountId: Int,
    private val eventLoop: RpcEventLoop,
) : MessageRepository {
    override fun getMessageIds(chatId: Int, addDaymarker: Boolean): IntArray {
        return try {
            (rpc.getMessageIds(accountId, chatId, false, addDaymarker) ?: emptyList())
                .map { it.toInt() }
                .toIntArray()
        } catch (_: RpcException) {
            intArrayOf()
        }
    }

    override fun getMessage(msgId: Int): ChatMessage? {
        return try {
            val msg = rpc.getMessage(accountId, msgId) ?: return null
            RpcMessageMapper.toChatMessage(msg)
        } catch (_: RpcException) {
            null
        }
    }

    override fun getStub(msgId: Int): MessageStub? {
        return try {
            val msg = rpc.getMessage(accountId, msgId) ?: return null
            RpcMessageMapper.toStub(msg)
        } catch (_: RpcException) {
            null
        }
    }

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

    override fun observeEngineEvents(onUpdate: () -> Unit): AutoCloseable =
        eventLoop.addListener(onUpdate)
}
