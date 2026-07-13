package com.polli.android.data.engine

import com.b44t.messenger.DcContext
import com.b44t.messenger.DcMsg
import com.polli.domain.model.chat.ChatListItem
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.MSG_ID_DAYMARKER
import com.polli.domain.model.chat.MessageReaction
import com.polli.domain.model.chat.MessageStub
import com.polli.domain.repository.MessageRepository

/**
 * Android feed hot path — direct JNI like Delta Chat [ConversationAdapter.getMsg].
 * Writes and reactions delegate to JSON-RPC ([writes]).
 */
class JniMessageRepository(
    private val dc: DcContext,
    private val writes: MessageRepository,
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

    override fun getMessageIds(chatId: Int, addDaymarker: Boolean): IntArray {
        val flags = if (addDaymarker) DcContext.DC_GCM_ADDDAYMARKER else 0
        return dc.getChatMsgs(chatId, flags, 0) ?: intArrayOf()
    }

    /** JNI list is int[] + day-marker ids — [ChatMessageStore] builds feed via [getMessageIds]. */
    override fun getMessageListItems(chatId: Int, addDaymarker: Boolean): List<ChatListItem>? = null

    override fun preloadMessages(msgIds: IntArray) {
        for (id in msgIds) {
            if (id <= MSG_ID_DAYMARKER) continue
            if (stubCache.containsKey(id) && messageCache.containsKey(id)) continue
            loadFromDc(id)
        }
    }

    override fun clearMessageCaches() {
        messageCache.clear()
        stubCache.clear()
        writes.clearMessageCaches()
    }

    override fun invalidateMessage(msgId: Int) {
        messageCache.remove(msgId)
        stubCache.remove(msgId)
        writes.invalidateMessage(msgId)
    }

    override fun getMessage(msgId: Int): ChatMessage? {
        messageCache[msgId]?.let { return it }
        return loadFromDc(msgId)?.first
    }

    override fun getStub(msgId: Int): MessageStub? {
        stubCache[msgId]?.let { return it }
        messageCache[msgId]?.let { return DcMessageMapper.stubFromMessage(it) }
        if (msgId <= MSG_ID_DAYMARKER) return null
        return loadFromDc(msgId)?.second
    }

    override fun getDraft(chatId: Int): String = dc.getDraft(chatId).text?.orEmpty() ?: ""

    override fun setDraft(chatId: Int, text: String, quotedMessageId: Int?) {
        writes.setDraft(chatId, text, quotedMessageId)
    }

    override fun clearDraft(chatId: Int) {
        writes.clearDraft(chatId)
    }

    override fun sendDraft(chatId: Int): Int? = writes.sendDraft(chatId)

    override fun deleteMessages(msgIds: IntArray) {
        writes.deleteMessages(msgIds)
        for (id in msgIds) {
            messageCache.remove(id)
            stubCache.remove(id)
        }
    }

    override fun marknoticedChat(chatId: Int) {
        dc.marknoticedChat(chatId)
    }

    override fun getFreshMessageCount(chatId: Int): Int = dc.getFreshMsgCount(chatId).coerceAtLeast(0)

    override fun sendReaction(msgId: Int, emoji: String) {
        writes.sendReaction(msgId, emoji)
        stubCache.remove(msgId)
        messageCache.remove(msgId)
    }

    override fun getMessageReactions(msgId: Int): List<MessageReaction> = writes.getMessageReactions(msgId)

    override fun sendMedia(
        chatId: Int,
        filePath: String,
        fileName: String?,
        mimeType: String?,
        caption: String?,
        viewType: String,
        quotedMessageId: Int?,
    ): Int? = writes.sendMedia(chatId, filePath, fileName, mimeType, caption, viewType, quotedMessageId)

    override fun editMessage(msgId: Int, newText: String) {
        writes.editMessage(msgId, newText)
        messageCache.remove(msgId)
        stubCache.remove(msgId)
    }

    override fun resendMessages(msgIds: IntArray) {
        writes.resendMessages(msgIds)
        for (id in msgIds) {
            messageCache.remove(id)
            stubCache.remove(id)
        }
    }

    override fun getMessageInfo(msgId: Int): String? = writes.getMessageInfo(msgId)

    override fun saveMessage(msgId: Int) = writes.saveMessage(msgId)

    override fun unsaveMessage(savedMsgId: Int) = writes.unsaveMessage(savedMsgId)

    override fun copyToBlobDir(localPath: String): String? = writes.copyToBlobDir(localPath)

    override fun observeEngineEvents(onUpdate: () -> Unit): AutoCloseable = writes.observeEngineEvents(onUpdate)

    private fun loadFromDc(msgId: Int): Pair<ChatMessage?, MessageStub?>? {
        if (msgId <= DcMsg.DC_MSG_ID_DAYMARKER) return null
        val msg = dc.getMsg(msgId)
        if (!msg.isOk) return null
        val chat = DcMessageMapper.toChatMessage(dc, msg)
        val stub = DcMessageMapper.toStub(dc, msg)
        chat?.let { messageCache[msgId] = it }
        stub?.let { stubCache[msgId] = it }
        return chat to stub
    }

    private companion object {
        const val MAX_CACHE_SIZE = 40
    }
}
