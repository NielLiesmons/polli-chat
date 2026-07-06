package com.polli.desktop

import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.ChatSessionInfo
import com.polli.domain.model.chat.MessageReaction
import com.polli.domain.model.chat.MessageStub
import com.polli.domain.repository.MessageRepository

/** Fake messages for desktop mock inbox chats. */
class MockMessageRepository : MessageRepository {
    private val now = System.currentTimeMillis() / 1000

    private data class MockMsg(
        val stub: MessageStub,
        val body: String,
    )

    private val threads =
        mutableMapOf(
            101 to
                mutableListOf(
                    mock(1, "Hey, are we still on for tomorrow?", false, "Alice Chen", now - 300),
                    mock(2, "Yes! See you at the cafe at 10.", true, "You", now - 240),
                    mock(3, "Perfect, I'll bring the notes.", false, "Alice Chen", now - 120),
                ),
            102 to
                mutableListOf(
                    mock(1, "Desktop KMP shell is live", false, "Bob", now - 3600),
                    mock(2, "Nice — shared ChatScreen next", true, "You", now - 3500),
                ),
        )

    override fun getMessageIds(chatId: Int, addDaymarker: Boolean): IntArray {
        val ids = threads[chatId]?.map { it.stub.id }?.toIntArray() ?: intArrayOf()
        return if (addDaymarker && ids.isNotEmpty()) intArrayOf(9) + ids else ids
    }

    override fun getMessage(msgId: Int): ChatMessage? {
        val entry = threads.values.flatten().firstOrNull { it.stub.id == msgId } ?: return null
        val s = entry.stub
        return ChatMessage(
            id = s.id,
            text = entry.body,
            timestamp = s.timestamp,
            isOutgoing = s.isOutgoing,
            authorId = if (s.isOutgoing) 1 else 2,
            authorName = s.authorName,
            authorColorSeed = s.authorColorSeed,
            quote = null,
            hasAttachment = false,
            isInfo = false,
            isEdited = s.isEdited,
        )
    }

    override fun getStub(msgId: Int): MessageStub? =
        threads.values.flatten().firstOrNull { it.stub.id == msgId }?.stub

    override fun getDraft(chatId: Int): String = ""

    override fun setDraft(chatId: Int, text: String, quotedMessageId: Int?) {}

    override fun clearDraft(chatId: Int) {}

    override fun sendDraft(chatId: Int): Int? {
        val list = threads.getOrPut(chatId) { mutableListOf() }
        val nextId = (list.maxOfOrNull { it.stub.id } ?: 0) + 1
        list += mock(nextId, "Sent from desktop mock", true, "You", System.currentTimeMillis() / 1000)
        return nextId
    }

    override fun deleteMessages(msgIds: IntArray) {}

    override fun marknoticedChat(chatId: Int) {}

    override fun getFreshMessageCount(chatId: Int): Int = 0

    override fun sendReaction(msgId: Int, emoji: String) {}

    override fun getMessageReactions(msgId: Int): List<MessageReaction> = emptyList()

    override fun sendMedia(
        chatId: Int,
        filePath: String,
        fileName: String?,
        mimeType: String?,
        caption: String?,
        viewType: String,
        quotedMessageId: Int?,
    ): Int? = null

    override fun editMessage(msgId: Int, newText: String) {
        for (list in threads.values) {
            val idx = list.indexOfFirst { it.stub.id == msgId }
            if (idx >= 0) {
                val old = list[idx]
                list[idx] =
                    old.copy(
                        body = newText.trim(),
                        stub = old.stub.copy(isEdited = true),
                    )
                return
            }
        }
    }

    override fun resendMessages(msgIds: IntArray) {}

    override fun getMessageInfo(msgId: Int): String? = null

    override fun saveMessage(msgId: Int) {}

    override fun unsaveMessage(savedMsgId: Int) {}

    override fun copyToBlobDir(localPath: String): String? = localPath

    override fun observeEngineEvents(onUpdate: () -> Unit): AutoCloseable = AutoCloseable { }

    private fun mock(
        id: Int,
        body: String,
        outgoing: Boolean,
        author: String,
        ts: Long,
    ): MockMsg =
        MockMsg(
            stub =
                MessageStub(
                    id = id,
                    timestamp = ts,
                    isOutgoing = outgoing,
                    authorId = if (outgoing) 1 else 2,
                    authorName = author,
                    authorColorSeed = author,
                    isEdited = false,
                    isInfo = false,
                    hasText = body.isNotBlank(),
                    hasAttachment = false,
                ),
            body = body,
        )
}
