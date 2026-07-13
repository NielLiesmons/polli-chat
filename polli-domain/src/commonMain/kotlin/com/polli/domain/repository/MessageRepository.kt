package com.polli.domain.repository

import com.polli.domain.model.chat.ChatListItem
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.MessageReaction
import com.polli.domain.model.chat.MessageStub

interface MessageRepository {
    fun getMessageIds(chatId: Int, addDaymarker: Boolean = true): IntArray

    /** Preferred feed source (day markers include timestamps). Null → fall back to [getMessageIds]. */
    fun getMessageListItems(chatId: Int, addDaymarker: Boolean = true): List<ChatListItem>? = null

    /** Batch-hydrate the message cache (DC `getMessages` / LRU warm-up). */
    fun preloadMessages(msgIds: IntArray) {}

    /** Clear repository-level LRU after structural feed changes. */
    fun clearMessageCaches() {}

    /** Drop one message from repository LRU (delivery state, reactions, edits). */
    fun invalidateMessage(msgId: Int) {}

    fun getMessage(msgId: Int): ChatMessage?

    fun getStub(msgId: Int): MessageStub?

    fun getDraft(chatId: Int): String

    fun setDraft(chatId: Int, text: String, quotedMessageId: Int? = null)

    fun clearDraft(chatId: Int)

    fun sendDraft(chatId: Int): Int?

    fun deleteMessages(msgIds: IntArray)

    fun marknoticedChat(chatId: Int)

    fun getFreshMessageCount(chatId: Int): Int

    fun sendReaction(msgId: Int, emoji: String)

    fun getMessageReactions(msgId: Int): List<MessageReaction>

    /** Send a file already on disk (blob dir path). Returns new message id or null. */
    fun sendMedia(
        chatId: Int,
        filePath: String,
        fileName: String?,
        mimeType: String?,
        caption: String?,
        viewType: String,
        quotedMessageId: Int? = null,
    ): Int?

    /** Request edit of an outgoing text message (chatmail edit protocol). */
    fun editMessage(msgId: Int, newText: String)

    fun resendMessages(msgIds: IntArray)

    fun getMessageInfo(msgId: Int): String?

    fun saveMessage(msgId: Int)

    fun unsaveMessage(savedMsgId: Int)

    /** Copy a local file into the engine blob directory; returns blob path. */
    fun copyToBlobDir(localPath: String): String?

    /** Notifies when chat-related engine events fire (all chats — filter by chatId in the UI). */
    fun observeEngineEvents(onUpdate: () -> Unit): AutoCloseable
}
