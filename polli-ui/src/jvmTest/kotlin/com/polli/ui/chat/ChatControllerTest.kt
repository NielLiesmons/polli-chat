package com.polli.ui.chat

import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.MessageReaction
import com.polli.domain.model.chat.MessageStub
import com.polli.domain.repository.MessageRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatControllerTest {
    @Test
    fun sendTextClearsDraftAndReloads() = runTest {
        val repo = FakeMessageRepository()
        val controller = ChatController(repo, this, formatDayLabel = { "Today" })
        controller.bind(chatId = 1)
        controller.updateDraft("hello")
        controller.send()
        assertEquals("", controller.draft)
        assertTrue(repo.sentDrafts.isNotEmpty())
        assertEquals(1, controller.reloadGeneration)
    }

    @Test
    fun editMessageUsesEditPathNotSendDraft() = runTest {
        val repo = FakeMessageRepository()
        val controller = ChatController(repo, this, formatDayLabel = { "Today" })
        controller.bind(chatId = 1)
        controller.beginEdit(sampleMessage(id = 42, text = "old"))
        controller.updateDraft("revised")
        controller.send()
        assertEquals(listOf(42 to "revised"), repo.edits)
        assertEquals(-1, controller.editingMessageId)
        assertTrue(repo.sentDrafts.isEmpty())
    }

    @Test
    fun engineEventInvalidatesCacheAndReloads() = runTest {
        val repo = FakeMessageRepository()
        val controller = ChatController(repo, this, formatDayLabel = { "Today" })
        controller.bind(chatId = 1)
        val before = controller.reloadGeneration
        repo.fireEvent()
        testScheduler.advanceUntilIdle()
        assertTrue(controller.reloadGeneration > before)
    }

    private fun sampleMessage(id: Int, text: String) =
        ChatMessage(
            id = id,
            text = text,
            timestamp = 1_700_000_000,
            isOutgoing = true,
            authorId = 1,
            authorName = "You",
            authorColorSeed = "you",
            quote = null,
            hasAttachment = false,
            isInfo = false,
            isEdited = false,
            outgoingState = null,
            viewType = "Text",
            fileName = null,
            filePath = null,
            width = 0,
            height = 0,
            durationMs = 0,
            savedMessageId = 0,
        )

    private class FakeMessageRepository : MessageRepository {
        val sentDrafts = mutableListOf<Int>()
        val edits = mutableListOf<Pair<Int, String>>()
        private var listener: (() -> Unit)? = null

        override fun getMessageIds(chatId: Int, addDaymarker: Boolean): IntArray = intArrayOf(42)

        override fun getMessage(msgId: Int): ChatMessage? =
            ChatMessage(
                id = msgId,
                text = "body",
                timestamp = 1,
                isOutgoing = true,
                authorId = 1,
                authorName = "You",
                authorColorSeed = "you",
                quote = null,
                hasAttachment = false,
                isInfo = false,
                isEdited = false,
                outgoingState = null,
                viewType = "Text",
                fileName = null,
                filePath = null,
                width = 0,
                height = 0,
                durationMs = 0,
                savedMessageId = 0,
            )

        override fun getStub(msgId: Int): MessageStub? =
            MessageStub(
                id = msgId,
                timestamp = 1,
                isOutgoing = true,
                authorId = 1,
                authorName = "You",
                authorColorSeed = "you",
                isEdited = false,
                isInfo = false,
                hasText = true,
                hasAttachment = false,
            )

        override fun getDraft(chatId: Int): String = ""

        override fun setDraft(chatId: Int, text: String, quotedMessageId: Int?) {}

        override fun clearDraft(chatId: Int) {}

        override fun sendDraft(chatId: Int): Int? {
            sentDrafts += chatId
            return 99
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
            edits += msgId to newText
        }

        override fun resendMessages(msgIds: IntArray) {}

        override fun getMessageInfo(msgId: Int): String? = null

        override fun saveMessage(msgId: Int) {}

        override fun unsaveMessage(savedMsgId: Int) {}

        override fun copyToBlobDir(localPath: String): String? = localPath

        override fun observeEngineEvents(onUpdate: () -> Unit): AutoCloseable {
            listener = onUpdate
            return AutoCloseable { listener = null }
        }

        fun fireEvent() {
            listener?.invoke()
        }
    }
}
