package com.polli.android.benchmark

import com.polli.domain.model.chat.ChatListItem
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.FeedItem
import com.polli.domain.model.chat.MessageReaction
import com.polli.domain.model.chat.MessageStub
import com.polli.domain.model.chat.OutgoingState
import com.polli.domain.repository.MessageRepository

/** In-memory message source for scroll/open perf benchmarks (zero RPC). */
class BenchmarkMessageRepository(
    private val chatId: Int = BENCHMARK_CHAT_ID,
    messageCount: Int = DEFAULT_MESSAGE_COUNT,
) : MessageRepository {
    private val ids: IntArray
    private val listItems: List<ChatListItem>
    private val messagesById: Map<Int, ChatMessage>
    private val stubsById: Map<Int, MessageStub>
    var bindPathCallCount: Int = 0
        private set

    init {
        val built = buildSyntheticChat(messageCount)
        ids = built.ids
        listItems = built.listItems
        messagesById = built.messages
        stubsById = built.stubs
    }

    private fun buildSyntheticChat(messageCount: Int): SyntheticChatData {
        val idArray = IntArray(messageCount) { index -> index + 1 }
        val items = idArray.map { ChatListItem.Message(it) }
        val messages = LinkedHashMap<Int, ChatMessage>(messageCount)
        val stubs = LinkedHashMap<Int, MessageStub>(messageCount)
        val baseTs = 1_700_000_000L
        for (i in 0 until messageCount) {
            val id = idArray[i]
            val outgoing = i % 3 != 0
            val authorId = if (outgoing) 1 else 2 + (i % 4)
            val text = "Benchmark message #$id — ${"lorem ".repeat(8 + (i % 5))}"
            val ts = baseTs + i * 90L
            val stub =
                MessageStub(
                    id = id,
                    timestamp = ts,
                    isOutgoing = outgoing,
                    authorId = authorId,
                    authorName = if (outgoing) "You" else "Contact $authorId",
                    authorColorSeed = if (outgoing) "you" else "contact-$authorId",
                    isEdited = false,
                    isInfo = false,
                    hasText = true,
                    hasAttachment = false,
                )
            val message =
                ChatMessage(
                    id = id,
                    text = text,
                    timestamp = ts,
                    isOutgoing = outgoing,
                    authorId = authorId,
                    authorName = stub.authorName,
                    authorColorSeed = stub.authorColorSeed,
                    quote = null,
                    hasAttachment = false,
                    isInfo = false,
                    isEdited = false,
                    outgoingState = if (outgoing) OutgoingState.Sent else null,
                )
            stubs[id] = stub
            messages[id] = message
        }
        return SyntheticChatData(idArray, items, messages, stubs)
    }

    override fun getMessageIds(chatId: Int, addDaymarker: Boolean): IntArray =
        if (chatId == this.chatId) ids else intArrayOf()

    override fun getMessageListItems(chatId: Int, addDaymarker: Boolean): List<ChatListItem>? =
        if (chatId == this.chatId) listItems else emptyList()

    override fun preloadMessages(msgIds: IntArray) {}

    override fun clearMessageCaches() {}

    override fun getMessage(msgId: Int): ChatMessage? {
        trackBindPath()
        return messagesById[msgId]
    }

    override fun getStub(msgId: Int): MessageStub? {
        trackBindPath()
        return stubsById[msgId]
    }

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

    fun resetBindPathCalls() {
        bindPathCallCount = 0
    }

    private fun trackBindPath() {
        bindPathCallCount++
    }

    private data class SyntheticChatData(
        val ids: IntArray,
        val listItems: List<ChatListItem>,
        val messages: Map<Int, ChatMessage>,
        val stubs: Map<Int, MessageStub>,
    )

    companion object {
        const val BENCHMARK_CHAT_ID = 4242
        const val DEFAULT_MESSAGE_COUNT = 1200
    }
}

/** Convenience for tests — count message rows in a feed list. */
fun List<FeedItem>.benchmarkMessageCount(): Int = count { it is FeedItem.Message }
