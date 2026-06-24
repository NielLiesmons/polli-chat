package com.polli.android.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.polli.android.bridge.ChatListMapper
import com.polli.android.bridge.InboxItem
import com.polli.core.chat.ChatCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.thoughtcrime.securesms.connect.DcEventCenter
import org.thoughtcrime.securesms.connect.DcHelper

class HomeViewModel(application: Application) : AndroidViewModel(application), DcEventCenter.DcEventDelegate {

    /** Read inside @Composable (e.g. `homeViewModel.items`) so Compose subscribes to updates. */
    var items by mutableStateOf<List<InboxItem>>(emptyList())
        private set
    var channels by mutableStateOf<List<InboxItem>>(emptyList())
        private set
    var spacesEmptyHint by mutableStateOf("No spaces yet. Your group chats appear here.")
        private set
    var mailEmptyHint by mutableStateOf("No mail chats yet.")
        private set

    private var searchQuery = ""
    private var registered = false

    init {
        registerEvents()
        reload()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        reload()
    }

    fun reload() {
        val ctx = getApplication<Application>()
        val q = searchQuery.ifBlank { null }
        val loaded = ChatListMapper.load(ctx, q)
        val channelRows = loaded.filter { it.category == ChatCategory.Channel }
        items = loaded
        channels = channelRows
        updateEmptyHints(loaded, channelRows)
    }

    private fun updateEmptyHints(loaded: List<InboxItem>, channelRows: List<InboxItem>) {
        val dc = DcHelper.getContext(getApplication())
        val connectivity = dc.connectivity
        val spaceCount = loaded.count { it.category == ChatCategory.Space }
        val mailCount = loaded.count { it.category == ChatCategory.Mail }
        spacesEmptyHint = when {
            connectivity < DcContext.DC_CONNECTIVITY_WORKING ->
                "Could not sync your chats. Check your connection and account settings."
            spaceCount == 0 && channelRows.isNotEmpty() ->
                "No group spaces yet. Broadcast channels and mailing lists appear in the story row above."
            spaceCount == 0 && mailCount > 0 ->
                "No group chats in your inbox. Your 1:1 chats are in the Mail tab."
            else -> "No spaces yet. Your group chats appear here."
        }
        mailEmptyHint = when {
            mailCount == 0 && spaceCount > 0 ->
                "No 1:1 chats yet. Your group chats are in the Spaces tab."
            else -> "No mail chats yet."
        }
    }

    private fun registerEvents() {
        if (registered) return
        val center = DcHelper.getEventCenter(getApplication())
        center.addMultiAccountObserver(DcContext.DC_EVENT_INCOMING_MSG, this)
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSGS_NOTICED, this)
        center.addMultiAccountObserver(DcContext.DC_EVENT_CHAT_DELETED, this)
        center.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this)
        center.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this)
        center.addObserver(DcContext.DC_EVENT_MSGS_CHANGED, this)
        center.addObserver(DcContext.DC_EVENT_MSG_DELIVERED, this)
        center.addObserver(DcContext.DC_EVENT_MSG_FAILED, this)
        center.addObserver(DcContext.DC_EVENT_MSG_READ, this)
        center.addObserver(DcContext.DC_EVENT_REACTIONS_CHANGED, this)
        center.addObserver(DcContext.DC_EVENT_CONNECTIVITY_CHANGED, this)
        center.addObserver(DcContext.DC_EVENT_SELFAVATAR_CHANGED, this)
        registered = true
    }

    private fun unregisterEvents() {
        if (!registered) return
        DcHelper.getEventCenter(getApplication()).removeObservers(this)
        registered = false
    }

    override fun handleEvent(event: DcEvent) {
        val dc = DcHelper.getContext(getApplication())
        if (event.accountId != dc.accountId && event.id != DcContext.DC_EVENT_CHAT_DELETED) {
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.Main) { reload() }
        }
    }

    override fun onCleared() {
        unregisterEvents()
        super.onCleared()
    }
}
