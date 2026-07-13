package com.polli.android.chat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.polli.android.connect.DcEventCenter
import com.polli.android.data.engine.PolliRepositories
import com.polli.android.platform.EngineBridge
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.FeedItem
import com.polli.domain.model.chat.displayIndexForMessage
import com.polli.domain.model.chat.messageIdAtDisplayIndex
import com.polli.android.platform.PlatformDates
import com.polli.ui.chat.ChatController

/**
 * Android lifecycle wrapper around shared [ChatController] (polli-ui).
 * Rich RecyclerView feed + overlays stay in [com.polli.android.chat.ChatScreen].
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private lateinit var controller: ChatController
    private var eventDelegate: DcEventCenter.DcEventDelegate? = null

    var chatId by mutableIntStateOf(-1)
        private set
    var overlayAnchor by mutableStateOf<BubbleOverlayAnchor?>(null)
        private set
    var highlightScrollIndex by mutableIntStateOf(-1)
        private set
    var unreadBelowCount by mutableIntStateOf(0)
        private set

    val reactionEpoch: SnapshotStateMap<Int, Int> = mutableStateMapOf()
    val messageEpoch: SnapshotStateMap<Int, Int> = mutableStateMapOf()

    private fun ensureController(): ChatController {
        if (!::controller.isInitialized) {
            controller =
                ChatController(
                    messages = PolliRepositories.messages(app),
                    scope = viewModelScope,
                    formatDayLabel = { ts -> PlatformDates.relativeDate(app, ts * 1000) },
                )
        }
        return controller
    }

    var feedItems: List<FeedItem>
        get() = ensureController().feedItems
        private set(_) {}

    var draft: String
        get() = ensureController().draft
        private set(_) {}

    var replyTo: ChatMessage?
        get() = ensureController().replyTo
        private set(_) {}

    var highlightId: Int
        get() = ensureController().highlightId
        private set(_) {}

    var reloadGeneration: Int
        get() = ensureController().reloadGeneration
        private set(_) {}

    var contentGeneration: Int
        get() = ensureController().contentGeneration
        private set(_) {}

    var reactionPulse: com.polli.ui.chat.ReactionPulse?
        get() = ensureController().reactionPulse
        private set(_) {}

    var initialScrollIndex: Int
        get() = ensureController().initialScrollIndex
        private set(_) {}

    var pendingFirstLoadScroll: Boolean
        get() = ensureController().pendingFirstLoadScroll
        private set(_) {}

    var composerGeneration: Int
        get() = ensureController().composerGeneration
        private set(_) {}

    val isEditingMessage: Boolean
        get() = ensureController().editingMessageId > 0

    fun consumeScrollToBottomOnReload(): Boolean = ensureController().consumeScrollToBottomOnReload()

    fun addUnreadBelow(delta: Int) {
        if (delta > 0) unreadBelowCount += delta
    }

    fun clearUnreadBelow() {
        unreadBelowCount = 0
    }

    fun onScrolledToBottom() {
        clearUnreadBelow()
    }

    fun getChatMessage(msgId: Int): ChatMessage? = ensureController().getChatMessage(msgId)

    fun getStub(msgId: Int) = ensureController().getStub(msgId)

    fun displayIndexForMsgId(msgId: Int): Int = feedItems.displayIndexForMessage(msgId)

    fun bind(
        chatId: Int,
        initialDraft: String? = null,
        startingPosition: Int = -1,
        fromArchived: Boolean = false,
    ) {
        this.chatId = chatId
        val c = ensureController()
        c.bind(chatId, initialDraft, startingPosition, fromArchived)
        registerChatEvents()
        if (startingPosition >= 0) {
            val rows = c.feedItems.filterIsInstance<FeedItem.Message>()
            if (rows.isNotEmpty()) {
                val chronIdx = (rows.size - 1 - startingPosition).coerceIn(0, rows.lastIndex)
                highlightScrollIndex = c.displayIndexForMsgId(rows[chronIdx].msgId)
            }
        }
    }

    fun bumpMessageEpoch(msgId: Int) {
        messageEpoch[msgId] = (messageEpoch[msgId] ?: 0) + 1
    }

    fun reactionEpochFor(msgId: Int): Int = reactionEpoch[msgId] ?: 0

    fun clearFirstLoadScroll() {
        ensureController().clearFirstLoadScroll()
        highlightScrollIndex = -1
    }

    fun updateDraft(text: String) = ensureController().updateDraft(text)

    fun setReply(message: ChatMessage?) {
        ensureController().setReply(message)
        overlayAnchor = null
    }

    fun showOverlay(message: ChatMessage, tapX: Float, tapY: Float) {
        overlayAnchor = BubbleOverlayAnchor(message, tapX, tapY)
    }

    fun dismissOverlay() {
        overlayAnchor = null
    }

    fun sendReaction(msgId: Int, emoji: String) {
        RecentEmojiStore.record(app, emoji)
        ensureController().sendReaction(msgId, emoji)
        MessageReactions.invalidateSummary(msgId)
        ensureController().refreshMessageRow(msgId)
        reactionEpoch[msgId] = (reactionEpoch[msgId] ?: 0) + 1
    }

    fun beginEdit(msg: ChatMessage) {
        ensureController().beginEdit(msg)
        overlayAnchor = null
    }

    fun cancelEdit() = ensureController().cancelEdit()

    fun notifyOutboundSent(sentMsgId: Int? = null) = ensureController().notifyOutboundSent(sentMsgId)

    fun highlightMessage(msgId: Int) = ensureController().highlightMessage(msgId)

    fun jumpToMessage(msgId: Int, onScroll: (displayIndex: Int) -> Unit) {
        val idx = ensureController().jumpToMessage(msgId)
        if (idx >= 0) onScroll(idx)
    }

    fun messageIdAtDisplayIndex(displayIndex: Int): Int? =
        feedItems.messageIdAtDisplayIndex(displayIndex)

    fun send() = ensureController().send()

    fun clearAfterSend(reload: Boolean = true) = ensureController().clearAfterSend(reload)

    fun deleteMessage(msgId: Int) {
        ensureController().deleteMessage(msgId)
        overlayAnchor = null
    }

    fun reload(markRead: Boolean = true) = ensureController().reload(markRead)

    private fun registerChatEvents() {
        unregisterChatEvents()
        if (chatId <= 0) return
        val center = EngineBridge.getEventCenter(app)
        val delegate = DcEventCenter.DcEventDelegate { event -> handleDcEvent(event) }
        eventDelegate = delegate
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSG_DELIVERED, delegate)
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSG_READ, delegate)
        center.addMultiAccountObserver(DcContext.DC_EVENT_REACTIONS_CHANGED, delegate)
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSGS_CHANGED, delegate)
    }

    private fun unregisterChatEvents() {
        val delegate = eventDelegate ?: return
        EngineBridge.getEventCenter(app).removeObservers(delegate)
        eventDelegate = null
    }

    private fun handleDcEvent(event: DcEvent) {
        if (chatId <= 0) return
        when (event.id) {
            DcContext.DC_EVENT_MSG_DELIVERED,
            DcContext.DC_EVENT_MSG_READ,
            -> {
                val msgId = event.data1Int
                if (msgId > 0 && feedContains(msgId)) {
                    ensureController().refreshMessageRow(msgId)
                    bumpMessageEpoch(msgId)
                }
            }
            DcContext.DC_EVENT_REACTIONS_CHANGED -> {
                val msgId = event.data1Int
                val eventChatId = event.data2Int
                if (eventChatId == chatId && msgId > 0) {
                    MessageReactions.invalidateSummary(msgId)
                    ensureController().refreshMessageRow(msgId)
                    reactionEpoch[msgId] = (reactionEpoch[msgId] ?: 0) + 1
                }
            }
            DcContext.DC_EVENT_MSGS_CHANGED -> {
                val eventChatId = event.data1Int
                if (eventChatId == chatId) {
                    val msgId = event.data2Int
                    if (msgId > 0 && feedContains(msgId)) {
                        ensureController().refreshMessageRow(msgId)
                        bumpMessageEpoch(msgId)
                    }
                }
            }
        }
    }

    private fun feedContains(msgId: Int): Boolean =
        feedItems.any { it is FeedItem.Message && it.msgId == msgId }

    override fun onCleared() {
        unregisterChatEvents()
        if (::controller.isInitialized) controller.dispose()
        super.onCleared()
    }
}
