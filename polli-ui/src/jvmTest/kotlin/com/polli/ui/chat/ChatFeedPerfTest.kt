package com.polli.ui.chat

import com.polli.domain.model.chat.ChatListItem
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.FeedItem
import com.polli.domain.model.chat.MessageReaction
import com.polli.domain.model.chat.MessageStub
import com.polli.domain.model.chat.OutgoingState
import com.polli.domain.model.chat.layoutsForMessageStubs
import com.polli.domain.repository.MessageRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class ChatFeedPerfTest {
    @Test
    fun syncFeedIds2000MessagesUnder30Ms() {
        val repo = SyntheticPerfRepository(messageCount = 2000)
        val store = ChatMessageStore(repo) { "Today" }
        val elapsed =
            measureTimeMillis {
                store.syncFeedIds(chatId = 1, showNewMessages = false, freshCount = 0)
            }
        assertTrue(elapsed < 30, "syncFeedIds took ${elapsed}ms")
    }

    @Test
    fun layoutsFor2000StubsUnder10Ms() {
        val stubs = buildStubs(2000)
        val elapsed =
            measureTimeMillis {
                layoutsForMessageStubs(stubs)
            }
        assertTrue(elapsed < 10, "layoutsForMessageStubs took ${elapsed}ms")
    }

    @Test
    fun feedBuilderAttachNeighbors2000Under20Ms() {
        val ids = IntArray(2000) { it + 1 }
        val elapsed =
            measureTimeMillis {
                ChatFeedBuilder.build({ "Today" }, NoopRepository(ids), ids, false, 0)
            }
        assertTrue(elapsed < 20, "build feed took ${elapsed}ms")
    }

    private fun buildStubs(count: Int): List<MessageStub> {
        val baseTs = 1_700_000_000L
        return List(count) { i ->
            MessageStub(
                id = i + 1,
                timestamp = baseTs + i * 60L,
                isOutgoing = i % 2 == 0,
                authorId = if (i % 2 == 0) 1 else 2,
                authorName = if (i % 2 == 0) "You" else "Them",
                authorColorSeed = if (i % 2 == 0) "you" else "them",
                isEdited = false,
                isInfo = false,
                hasText = true,
                hasAttachment = false,
            )
        }
    }

    private class SyntheticPerfRepository(
        messageCount: Int,
    ) : MessageRepository {
        private val ids = IntArray(messageCount) { it + 1 }
        private val listItems = ids.map { ChatListItem.Message(it) }

        override fun getMessageIds(chatId: Int, addDaymarker: Boolean): IntArray = ids

        override fun getMessageListItems(chatId: Int, addDaymarker: Boolean): List<ChatListItem> = listItems

        override fun getMessage(msgId: Int): ChatMessage? = null

        override fun getStub(msgId: Int): MessageStub? = null

        override fun getDraft(chatId: Int): String = ""

        override fun setDraft(chatId: Int, text: String, quotedMessageId: Int?) {}

        override fun clearDraft(chatId: Int) {}

        override fun sendDraft(chatId: Int): Int? = null

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

        override fun editMessage(msgId: Int, newText: String) {}

        override fun resendMessages(msgIds: IntArray) {}

        override fun getMessageInfo(msgId: Int): String? = null

        override fun saveMessage(msgId: Int) {}

        override fun unsaveMessage(savedMsgId: Int) {}

        override fun copyToBlobDir(localPath: String): String? = localPath

        override fun observeEngineEvents(onUpdate: () -> Unit): AutoCloseable = AutoCloseable { }
    }

    private class NoopRepository(private val ids: IntArray) : MessageRepository {
        override fun getMessageIds(chatId: Int, addDaymarker: Boolean): IntArray = ids

        override fun getMessage(msgId: Int): ChatMessage? = null

        override fun getStub(msgId: Int): MessageStub? = null

        override fun getDraft(chatId: Int): String = ""

        override fun setDraft(chatId: Int, text: String, quotedMessageId: Int?) {}

        override fun clearDraft(chatId: Int) {}

        override fun sendDraft(chatId: Int): Int? = null

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

        override fun editMessage(msgId: Int, newText: String) {}

        override fun resendMessages(msgIds: IntArray) {}

        override fun getMessageInfo(msgId: Int): String? = null

        override fun saveMessage(msgId: Int) {}

        override fun unsaveMessage(savedMsgId: Int) {}

        override fun copyToBlobDir(localPath: String): String? = localPath

        override fun observeEngineEvents(onUpdate: () -> Unit): AutoCloseable = AutoCloseable { }
    }
}
