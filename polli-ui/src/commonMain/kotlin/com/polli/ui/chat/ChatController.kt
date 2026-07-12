package com.polli.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.FeedItem
import com.polli.domain.model.chat.displayIndexForMessage
import com.polli.domain.model.chat.messageIdAtDisplayIndex
import com.polli.domain.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReactionPulse(
    val msgId: Int,
    val emoji: String,
)

/**
 * Shared chat feed + composer state — used by Android [com.polli.android.chat.ChatViewModel]
 * and desktop [com.polli.desktop.DesktopChatScreen].
 */
class ChatController(
    private val messages: MessageRepository,
    private val scope: CoroutineScope,
    formatDayLabel: (Long) -> String = ::formatChatDayLabel,
) {
    var chatId by mutableIntStateOf(-1)
        private set
    var feedItems by mutableStateOf<List<FeedItem>>(emptyList())
        private set
    var draft by mutableStateOf("")
        private set
    var replyTo by mutableStateOf<ChatMessage?>(null)
        private set
    var editingMessageId by mutableIntStateOf(-1)
        private set
    var highlightId by mutableIntStateOf(-1)
        private set
    var reloadGeneration by mutableIntStateOf(0)
        private set
    var contentGeneration by mutableIntStateOf(0)
        private set
    var reactionPulse by mutableStateOf<ReactionPulse?>(null)
        private set
    var initialScrollIndex by mutableIntStateOf(0)
        private set
    var pendingFirstLoadScroll by mutableStateOf(true)
        private set
    var composerGeneration by mutableIntStateOf(0)
        private set

    private var showNewMessagesMarker by mutableStateOf(false)
    private var scrollToBottomOnReload = false

    private val store = ChatMessageStore(messages, formatDayLabel)
    private var eventObserver: AutoCloseable? = null
    private var registered = false
    private var reloadJob: Job? = null

    fun consumeScrollToBottomOnReload(): Boolean {
        if (!scrollToBottomOnReload) return false
        scrollToBottomOnReload = false
        return true
    }

    fun getChatMessage(msgId: Int): ChatMessage? = store.getMessage(msgId)

    fun getStub(msgId: Int) = store.getStub(msgId)

    fun displayIndexForMsgId(msgId: Int): Int = feedItems.displayIndexForMessage(msgId)

    fun bind(
        chatId: Int,
        initialDraft: String? = null,
        startingPosition: Int = -1,
        fromArchived: Boolean = false,
    ) {
        if (this.chatId == chatId && registered) return
        this.chatId = chatId
        editingMessageId = -1
        draft = initialDraft?.takeIf { it.isNotBlank() } ?: messages.getDraft(chatId)
        val freshMsgs = freshMessageCount()
        showNewMessagesMarker = freshMsgs > 0
        registerEvents()
        store.reset()
        feedItems =
            store.syncFeedIds(chatId, showNewMessagesMarker, freshMsgs)
                ?: store.buildFeed(showNewMessagesMarker, freshMsgs)
        initialScrollIndex =
            resolveInitialScrollIndex(
                items = feedItems,
                startingPosition = startingPosition,
                freshMsgs = freshMsgs,
            )
        pendingFirstLoadScroll = true
        reloadGeneration++
        if (!fromArchived) {
            scope.launch(Dispatchers.Default) {
                messages.marknoticedChat(chatId)
            }
        }
        scope.launch {
            withContext(Dispatchers.Default) {
                store.preloadStubsAroundDisplayIndex(initialScrollIndex, radius = 40)
            }
            feedItems = store.rebuildGroupLayouts()
            reloadGeneration++
            launch(Dispatchers.Default) {
                store.preloadStubs()
            }
        }
    }

    fun clearFirstLoadScroll() {
        pendingFirstLoadScroll = false
    }

    fun updateDraft(text: String) {
        draft = text
        persistDraft()
    }

    private fun bumpComposer() {
        composerGeneration++
    }

    fun setReply(message: ChatMessage?) {
        replyTo = message
        editingMessageId = -1
        persistDraft()
        bumpComposer()
    }

    fun beginEdit(message: ChatMessage) {
        editingMessageId = message.id
        draft = message.text
        replyTo = null
        messages.clearDraft(chatId)
        bumpComposer()
    }

    fun cancelEdit() {
        if (editingMessageId <= 0) return
        editingMessageId = -1
        draft = messages.getDraft(chatId)
        bumpComposer()
    }

    fun sendReaction(msgId: Int, emoji: String) {
        messages.sendReaction(msgId, emoji)
        reactionPulse = ReactionPulse(msgId, emoji)
        scope.launch {
            delay(700)
            if (reactionPulse?.msgId == msgId && reactionPulse?.emoji == emoji) {
                reactionPulse = null
            }
        }
    }

    fun highlightMessage(msgId: Int) {
        highlightId = msgId
        scope.launch {
            delay(1600)
            if (highlightId == msgId) highlightId = -1
        }
    }

    fun jumpToMessage(msgId: Int): Int {
        val idx = displayIndexForMsgId(msgId)
        if (idx >= 0) highlightMessage(msgId)
        return idx
    }

    fun messageIdAtDisplayIndex(displayIndex: Int): Int? =
        feedItems.messageIdAtDisplayIndex(displayIndex)

    fun send() {
        if (chatId <= 0) return
        val text = draft.trim()
        if (text.isEmpty()) return
        val editId = editingMessageId
        if (editId > 0) {
            messages.editMessage(editId, text)
            store.invalidateMessage(editId)
            store.invalidateStub(editId)
            editingMessageId = -1
            draft = ""
            messages.clearDraft(chatId)
            scrollToBottomOnReload = true
            scheduleReload(markRead = true)
            bumpComposer()
            return
        }
        messages.setDraft(chatId, text, replyTo?.id)
        val sentMsgId = messages.sendDraft(chatId)
        clearAfterSend(reload = false)
        reloadAfterOutbound(sentMsgId)
    }

    fun notifyOutboundSent(sentMsgId: Int? = null) {
        reloadAfterOutbound(sentMsgId)
    }

    fun clearAfterSend(reload: Boolean = true) {
        editingMessageId = -1
        draft = ""
        replyTo = null
        messages.clearDraft(chatId)
        scrollToBottomOnReload = true
        bumpComposer()
        if (reload) {
            reloadAfterOutbound(sentMsgId = null)
        }
    }

    fun deleteMessage(msgId: Int) {
        messages.deleteMessages(intArrayOf(msgId))
        scheduleReload(markRead = false)
    }

    fun reload(markRead: Boolean = true) {
        scheduleReload(markRead)
    }

    fun dispose() {
        unregisterEvents()
        persistDraft()
    }

    private fun freshMessageCount(): Int =
        if (chatId > 0) messages.getFreshMessageCount(chatId) else 0

    private fun registerEvents() {
        if (registered) return
        eventObserver =
            messages.observeEngineEvents {
                scheduleReload(markRead = false)
            }
        registered = true
    }

    private fun unregisterEvents() {
        if (!registered) return
        eventObserver?.close()
        eventObserver = null
        registered = false
    }

    private fun scheduleReload(markRead: Boolean) {
        reloadJob?.cancel()
        reloadJob =
            scope.launch {
                applyFeedReload(markRead = markRead, sentMsgId = null, allowAsyncRetry = true)
            }
    }

    /** Synchronous feed refresh after send — DC reloadList runs on main when msgs change. */
    private fun reloadAfterOutbound(sentMsgId: Int?) {
        scrollToBottomOnReload = true
        val applied =
            applyFeedReload(markRead = true, sentMsgId = sentMsgId, allowAsyncRetry = sentMsgId == null)
        if (!applied && sentMsgId == null) {
            scheduleReload(markRead = true)
        }
    }

    private fun applyFeedReload(
        markRead: Boolean,
        sentMsgId: Int?,
        allowAsyncRetry: Boolean,
    ): Boolean {
        if (chatId <= 0) return false
        val fresh = freshMessageCount()
        if (markRead) messages.marknoticedChat(chatId)
        val synced = store.syncFeedIds(chatId, showNewMessagesMarker, fresh)
        when {
            synced != null -> {
                feedItems = synced
                reloadGeneration++
                sentMsgId?.let { store.preloadMessages(intArrayOf(it)) }
                    ?: scope.launch(Dispatchers.Default) { store.preloadStubs() }
                return true
            }
            sentMsgId != null -> {
                feedItems = store.appendOutgoingMessage(sentMsgId, showNewMessagesMarker, fresh)
                reloadGeneration++
                store.preloadMessages(intArrayOf(sentMsgId))
                return true
            }
            else -> {
                store.clearMessageCache()
                contentGeneration++
                return !allowAsyncRetry
            }
        }
    }

    private fun persistDraft() {
        if (chatId <= 0 || editingMessageId > 0) return
        val trimmed = draft.trim()
        if (trimmed.isEmpty() && replyTo == null) {
            messages.clearDraft(chatId)
            return
        }
        messages.setDraft(chatId, trimmed, replyTo?.id)
    }

    private fun resolveInitialScrollIndex(
        items: List<FeedItem>,
        startingPosition: Int,
        freshMsgs: Int,
    ): Int {
        val messageRows = items.filterIsInstance<FeedItem.Message>()
        if (messageRows.isEmpty()) return 0
        val targetMsgId =
            when {
                startingPosition >= 0 -> {
                    val msgIndex = (messageRows.size - 1 - startingPosition).coerceIn(0, messageRows.lastIndex)
                    messageRows[msgIndex].msgId
                }
                freshMsgs > 0 -> {
                    val msgIndex = (messageRows.size - freshMsgs).coerceIn(0, messageRows.lastIndex)
                    messageRows[msgIndex].msgId
                }
                else -> messageRows.last().msgId
            }
        return items.displayIndexForMessage(targetMsgId).coerceAtLeast(0)
    }
}
