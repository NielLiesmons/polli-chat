package com.polli.android.chat

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.b44t.messenger.DcMsg
import com.polli.android.chat.MessageLoader.buildFeedItems
import com.polli.android.chat.MessageLoader.loadMessages
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    var messages by mutableStateOf<List<ChatMessage>>(emptyList())
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

    /** Bumped on every reload so the feed can preserve scroll or stick to bottom. */
    var reloadGeneration by mutableIntStateOf(0)
        private set

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

    private var dcContext: DcContext? = null
    private var registered = false

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
        initialScrollIndex = when {
            startingPosition >= 0 -> startingPosition
            freshMsgs > 0 -> freshMsgs - 1
            else -> 0
        }
        highlightScrollIndex = startingPosition.takeIf { it >= 0 } ?: -1
        pendingFirstLoadScroll = true
        registerEvents()
        reload(markRead = !fromArchived)
    }

    fun clearFirstLoadScroll() {
        pendingFirstLoadScroll = false
        highlightScrollIndex = -1
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
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSG_READ, this)
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
        val dc = dcContext ?: return
        when (event.id) {
            DcContext.DC_EVENT_INCOMING_MSG,
            DcContext.DC_EVENT_MSG_READ,
            -> {
                val msg = dc.getMsg(event.data1Int)
                if (msg.isOk && msg.chatId == chatId) {
                    viewModelScope.launch { reload(markRead = true) }
                }
            }
            DcContext.DC_EVENT_CHAT_MODIFIED -> {
                if (event.data1Int == chatId) {
                    viewModelScope.launch { reload(markRead = false) }
                }
            }
            DcContext.DC_EVENT_CONTACTS_CHANGED,
            DcContext.DC_EVENT_CHAT_EPHEMERAL_TIMER_MODIFIED,
            -> viewModelScope.launch { reload(markRead = false) }
            DcContext.DC_EVENT_REACTIONS_CHANGED -> {
                if (event.data1Int == chatId) {
                    viewModelScope.launch { reload(markRead = false) }
                }
            }
        }
    }

    fun reload(markRead: Boolean = true) {
        val dc = dcContext ?: return
        if (markRead) {
            dc.marknoticedChat(chatId)
        }
        val loaded = loadMessages(dc, chatId)
        messages = loaded
        feedItems = buildFeedItems(loaded)
        reloadGeneration++
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

    fun showOverlay(message: ChatMessage) {
        overlayAnchor = BubbleOverlayAnchor(message)
    }

    fun dismissOverlay() {
        overlayAnchor = null
    }

    fun sendReaction(msgId: Int, emoji: String) {
        val app = getApplication<Application>()
        MessageReactions.sendReaction(app, msgId, emoji)
        reactionPulse = ReactionPulse(msgId, emoji)
        reload(markRead = false)
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
        val feedIndex = feedItems.indexOfFirst { item ->
            when (item) {
                is FeedItem.Message -> item.message.id == msgId
                is FeedItem.IncomingStack -> item.messages.any { it.first.id == msgId }
                else -> false
            }
        }
        if (feedIndex >= 0) {
            onScroll(displayIndexForFeedIndex(feedIndex, feedItems.size))
            highlightMessage(msgId)
        }
    }

    fun messageIdAtDisplayIndex(displayIndex: Int): Int? {
        val feedIndex = feedItems.size - 1 - displayIndex
        val item = feedItems.getOrNull(feedIndex) ?: return null
        return when (item) {
            is FeedItem.Message -> item.message.id
            is FeedItem.IncomingStack -> item.messages.first().first.id
            else -> null
        }
    }

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
        draft = ""
        replyTo = null
        dc.setDraft(chatId, null)
        reload()
    }

    fun deleteMessage(msgId: Int) {
        dcContext?.deleteMsgs(intArrayOf(msgId))
        overlayAnchor = null
        reload()
    }

    override fun onCleared() {
        unregisterEvents()
        persistDraft()
        super.onCleared()
    }
}
