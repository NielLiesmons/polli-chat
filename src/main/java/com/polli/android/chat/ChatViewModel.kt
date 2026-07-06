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
import com.b44t.messenger.DcMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.thoughtcrime.securesms.connect.DcEventCenter
import org.thoughtcrime.securesms.connect.DcHelper
import chat.delta.rpc.RpcException

data class ReactionPulse(
    val msgId: Int,
    val emoji: String,
)

class ChatViewModel(application: Application) : AndroidViewModel(application), DcEventCenter.DcEventDelegate {

    var chatId by mutableIntStateOf(-1)
        private set
    var feedItems by mutableStateOf<List<FeedItem>>(emptyList())
        private set
    var draft by mutableStateOf("")
        private set
    var replyTo by mutableStateOf<ChatMessage?>(null)
        private set
    var overlayAnchor by mutableStateOf<BubbleOverlayAnchor?>(null)
        private set
    var highlightId by mutableIntStateOf(-1)
        private set

    /** Bumped on feed structure changes so scroll logic can preserve position or stick to bottom. */
    var reloadGeneration by mutableIntStateOf(0)
        private set

    /** Per-message reaction refresh — avoids reloading the whole feed (DC bumps one row). */
    val reactionEpoch: SnapshotStateMap<Int, Int> = mutableStateMapOf()

    /** Per-message content refresh (read receipts, delivery, edits). */
    val messageEpoch: SnapshotStateMap<Int, Int> = mutableStateMapOf()

    /** One-shot pop animation target after sending a reaction. */
    var reactionPulse by mutableStateOf<ReactionPulse?>(null)
        private set

    /** Display index (newest = 0) to open at — DC starting position / fresh-msg boundary. */
    var initialScrollIndex by mutableIntStateOf(0)
        private set

    var highlightScrollIndex by mutableIntStateOf(-1)
        private set

    var editingMsgId by mutableIntStateOf(-1)
        private set

    var pendingFirstLoadScroll by mutableStateOf(true)
        private set

    var unreadBelowCount by mutableIntStateOf(0)
        private set

    /** Inline "New messages" pill — hidden after the user scrolls to the bottom. */
    private var showNewMessagesMarker by mutableStateOf(false)

    /** Set before [scheduleReload] when the user sends — feed should stick to bottom after reload. */
    private var scrollToBottomOnReload = false

    private val store = ChatMessageStore()
    private var dcContext: DcContext? = null
    private var registered = false
    private var reloadJob: Job? = null

    fun consumeScrollToBottomOnReload(): Boolean {
        if (!scrollToBottomOnReload) return false
        scrollToBottomOnReload = false
        return true
    }

    fun addUnreadBelow(delta: Int) {
        if (delta > 0) unreadBelowCount += delta
    }

    fun clearUnreadBelow() {
        unreadBelowCount = 0
    }

    fun onScrolledToBottom() {
        if (showNewMessagesMarker) {
            showNewMessagesMarker = false
            val dc = dcContext
            val ctx = getApplication<Application>()
            if (dc != null) {
                feedItems = store.buildFeed(ctx, dc, showNewMessages = false)
                reloadGeneration++
            }
        }
        clearUnreadBelow()
    }

    fun getChatMessage(msgId: Int): ChatMessage? {
        val dc = dcContext ?: return null
        return store.getMessage(dc, msgId)
    }

    fun messageIds(): IntArray = store.messageIds()

    fun displayIndexForMsgId(msgId: Int): Int = feedItems.displayIndexForMessage(msgId)

    /**
     * Polli grouping for one row — loads at most three stubs (self + neighbors), cached after first read.
     */
    fun layoutForMessage(
        msgId: Int,
        olderMsgId: Int?,
        newerMsgId: Int?,
    ): MessageGroupLayout {
        val dc = dcContext ?: return MessageGroupLayout()
        val self = store.getStub(dc, msgId) ?: return MessageGroupLayout()
        val olderStub = olderMsgId?.let { store.getStub(dc, it) }
        val newerStub = newerMsgId?.let { store.getStub(dc, it) }
        return layoutBetweenNeighbors(olderStub, self, newerStub)
    }

    fun bind(
        chatId: Int,
        initialDraft: String? = null,
        startingPosition: Int = -1,
        fromArchived: Boolean = false,
    ) {
        if (this.chatId == chatId && registered) return
        this.chatId = chatId
        val ctx = getApplication<Application>()
        dcContext = DcHelper.getContext(ctx)
        draft = initialDraft?.takeIf { it.isNotBlank() }
            ?: dcContext?.getDraft(chatId)?.text?.orEmpty()
            ?: ""
        val freshMsgs = freshMessageCount(chatId)
        showNewMessagesMarker = freshMsgs > 0
        registerEvents()
        val dc = dcContext!!
        store.reset()
        feedItems =
            store.syncFeedIds(ctx, dc, chatId, showNewMessagesMarker)
                ?: store.buildFeed(ctx, dc, showNewMessagesMarker)
        initialScrollIndex =
            resolveInitialScrollIndex(
                items = feedItems,
                startingPosition = startingPosition,
                freshMsgs = freshMsgs,
            )
        highlightScrollIndex =
            if (startingPosition >= 0) {
                resolveInitialScrollIndex(feedItems, startingPosition, freshMsgs = 0)
            } else {
                -1
            }
        pendingFirstLoadScroll = true
        store.preloadStubsAroundDisplayIndex(dc, initialScrollIndex)
        reloadGeneration++
        if (!fromArchived) {
            viewModelScope.launch(Dispatchers.Main.immediate) {
                dc.marknoticedChat(chatId)
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            store.preloadStubs(dc)
        }
    }

    fun reactionEpochFor(msgId: Int): Int = reactionEpoch[msgId] ?: 0

    fun clearFirstLoadScroll() {
        pendingFirstLoadScroll = false
        highlightScrollIndex = -1
    }

    private fun resolveInitialScrollIndex(
        items: List<FeedItem>,
        startingPosition: Int,
        freshMsgs: Int,
    ): Int {
        val messages = items.filterIsInstance<FeedItem.Message>()
        if (messages.isEmpty()) return 0
        val targetMsgId =
            when {
                startingPosition >= 0 -> {
                    val msgIndex = (messages.size - 1 - startingPosition).coerceIn(0, messages.lastIndex)
                    messages[msgIndex].msgId
                }
                freshMsgs > 0 -> {
                    val msgIndex = (messages.size - freshMsgs).coerceIn(0, messages.lastIndex)
                    messages[msgIndex].msgId
                }
                else -> messages.last().msgId
            }
        return items.displayIndexForMessage(targetMsgId).coerceAtLeast(0)
    }

    private fun freshMessageCount(chatId: Int): Int = try {
        val rpc = DcHelper.getRpc(getApplication())
        rpc.getFreshMsgCnt(rpc.selectedAccountId, chatId) ?: 0
    } catch (_: RpcException) {
        0
    }

    private fun registerEvents() {
        if (registered) return
        val center = DcHelper.getEventCenter(getApplication())
        center.addMultiAccountObserver(DcContext.DC_EVENT_INCOMING_MSG, this)
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSGS_CHANGED, this)
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSG_READ, this)
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSG_DELIVERED, this)
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSG_FAILED, this)
        center.addMultiAccountObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this)
        center.addMultiAccountObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this)
        center.addMultiAccountObserver(DcContext.DC_EVENT_CHAT_EPHEMERAL_TIMER_MODIFIED, this)
        center.addMultiAccountObserver(DcContext.DC_EVENT_REACTIONS_CHANGED, this)
        registered = true
    }

    private fun unregisterEvents() {
        if (!registered) return
        DcHelper.getEventCenter(getApplication()).removeObservers(this)
        registered = false
    }

    override fun handleEvent(event: DcEvent) {
        when (event.id) {
            DcContext.DC_EVENT_INCOMING_MSG -> {
                if (event.data1Int == chatId) {
                    scheduleAppend(event.data2Int, markRead = true)
                }
            }
            DcContext.DC_EVENT_MSGS_CHANGED -> {
                if (event.data1Int == 0 || event.data1Int == chatId) {
                    scheduleReload(markRead = false)
                }
            }
            DcContext.DC_EVENT_MSG_READ,
            DcContext.DC_EVENT_MSG_DELIVERED,
            DcContext.DC_EVENT_MSG_FAILED,
            -> {
                if (event.data1Int == chatId) {
                    invalidateMessage(event.data2Int)
                }
            }
            DcContext.DC_EVENT_CHAT_MODIFIED -> {
                if (event.data1Int == chatId) {
                    scheduleReload(markRead = false)
                }
            }
            DcContext.DC_EVENT_CONTACTS_CHANGED,
            DcContext.DC_EVENT_CHAT_EPHEMERAL_TIMER_MODIFIED,
            -> scheduleReload(markRead = false)
            DcContext.DC_EVENT_REACTIONS_CHANGED -> {
                if (event.data1Int == chatId) {
                    bumpReactionEpoch(event.data2Int)
                }
            }
        }
    }

    fun reload(markRead: Boolean = true) {
        scheduleReload(markRead)
    }

    private fun scheduleReload(markRead: Boolean) {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            val dc = dcContext ?: return@launch
            val ctx = getApplication<Application>()
            if (markRead) {
                withContext(Dispatchers.Main.immediate) {
                    dc.marknoticedChat(chatId)
                }
            }
            if (feedItems.isEmpty()) {
                feedItems =
                    store.syncFeedIds(ctx, dc, chatId, showNewMessagesMarker)
                        ?: store.buildFeed(ctx, dc, showNewMessagesMarker)
                reloadGeneration++
            } else {
                store.syncFeedIds(ctx, dc, chatId, showNewMessagesMarker)?.let { items ->
                    feedItems = items
                    reloadGeneration++
                }
            }
            withContext(Dispatchers.Default) {
                store.preloadStubs(dc)
            }
        }
    }

    private fun scheduleAppend(msgId: Int, markRead: Boolean) {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            val dc = dcContext ?: return@launch
            val ctx = getApplication<Application>()
            if (markRead) {
                withContext(Dispatchers.Main.immediate) {
                    dc.marknoticedChat(chatId)
                }
            }
            val items =
                withContext(Dispatchers.Default) {
                    store.tryAppendIncoming(ctx, dc, chatId, msgId, showNewMessagesMarker)
                        ?: store.syncFeedIds(ctx, dc, chatId, showNewMessagesMarker)
                }
            if (items != null) {
                feedItems = items
                reloadGeneration++
            }
            withContext(Dispatchers.Default) {
                store.preloadStubs(dc)
            }
        }
    }

    private fun invalidateMessage(msgId: Int) {
        store.invalidateMessage(msgId)
        store.invalidateStub(msgId)
        messageEpoch[msgId] = (messageEpoch[msgId] ?: 0) + 1
    }

    private     fun bumpReactionEpoch(msgId: Int) {
        MessageReactions.invalidateSummary(msgId)
        reactionEpoch[msgId] = (reactionEpoch[msgId] ?: 0) + 1
    }

    fun updateDraft(text: String) {
        draft = text
        persistDraft()
    }

    private fun persistDraft() {
        val dc = dcContext ?: return
        val trimmed = draft.trim()
        if (trimmed.isEmpty() && replyTo == null) {
            dc.setDraft(chatId, null)
            return
        }
        val msg = DcMsg(dc, DcMsg.DC_MSG_TEXT)
        msg.setText(trimmed)
        replyTo?.let { reply ->
            val quoted = dc.getMsg(reply.id)
            if (quoted.isOk) msg.setQuote(quoted)
        }
        dc.setDraft(chatId, msg)
    }

    fun setReply(message: ChatMessage?) {
        replyTo = message
        overlayAnchor = null
        persistDraft()
    }

    fun showOverlay(message: ChatMessage, tapX: Float, tapY: Float) {
        overlayAnchor = BubbleOverlayAnchor(message, tapX, tapY)
    }

    fun dismissOverlay() {
        overlayAnchor = null
    }

    fun sendReaction(msgId: Int, emoji: String) {
        val app = getApplication<Application>()
        MessageReactions.sendReaction(app, msgId, emoji)
        reactionPulse = ReactionPulse(msgId, emoji)
        bumpReactionEpoch(msgId)
        viewModelScope.launch {
            delay(700)
            if (reactionPulse?.msgId == msgId && reactionPulse?.emoji == emoji) {
                reactionPulse = null
            }
        }
    }

    fun beginEdit(msg: DcMsg) {
        editingMsgId = msg.id
        draft = msg.text?.orEmpty() ?: ""
        replyTo = null
        overlayAnchor = null
        persistDraft()
    }

    fun highlightMessage(msgId: Int) {
        highlightId = msgId
        viewModelScope.launch {
            delay(1600)
            if (highlightId == msgId) highlightId = -1
        }
    }

    fun jumpToMessage(msgId: Int, onScroll: (displayIndex: Int) -> Unit) {
        val idx = displayIndexForMsgId(msgId)
        if (idx >= 0) {
            onScroll(idx)
            highlightMessage(msgId)
        }
    }

    fun messageIdAtDisplayIndex(displayIndex: Int): Int? =
        feedItems.messageIdAtDisplayIndex(displayIndex)

    fun send() {
        val dc = dcContext ?: return
        val text = draft.trim()
        if (text.isEmpty()) return
        val msg = DcMsg(dc, DcMsg.DC_MSG_TEXT)
        msg.setText(text)
        replyTo?.let { reply ->
            val quoted = dc.getMsg(reply.id)
            if (quoted.isOk) msg.setQuote(quoted)
        }
        dc.sendMsg(chatId, msg)
        clearAfterSend()
    }

    fun clearAfterSend() {
        draft = ""
        replyTo = null
        dcContext?.setDraft(chatId, null)
        scrollToBottomOnReload = true
        scheduleReload(markRead = true)
    }

    fun deleteMessage(msgId: Int) {
        dcContext?.deleteMsgs(intArrayOf(msgId))
        overlayAnchor = null
        scheduleReload(markRead = false)
    }

    override fun onCleared() {
        unregisterEvents()
        persistDraft()
        super.onCleared()
    }
}
