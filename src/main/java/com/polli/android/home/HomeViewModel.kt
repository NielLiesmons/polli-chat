package com.polli.android.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.b44t.messenger.DcContext
import com.polli.android.data.engine.PolliRepositories
import com.polli.core.chat.ChatCategory
import com.polli.domain.model.InboxItem
import com.polli.domain.repository.ChatRepository
import com.polli.android.platform.EngineBridge

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository = PolliRepositories.chat(application)

    var items by mutableStateOf<List<InboxItem>>(emptyList())
        private set
    var channels by mutableStateOf<List<InboxItem>>(emptyList())
        private set
    var homeEmptyHint by mutableStateOf("No chats yet. Your spaces and mail appear here.")
        private set

    private var searchQuery = ""
    private val inboxObserver = chatRepository.observeInbox { reload() }

    init {
        reload()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        reload()
    }

    fun reload() {
        val q = searchQuery.ifBlank { null }
        val loaded = chatRepository.loadInbox(q)
        val channelRows = loaded.filter { it.category == ChatCategory.Channel }
        items = loaded
        channels = channelRows
        updateEmptyHint(loaded, channelRows)
    }

    private fun updateEmptyHint(loaded: List<InboxItem>, channelRows: List<InboxItem>) {
        val dc = EngineBridge.getContext(getApplication())
        val connectivity = dc.connectivity
        val homeCount =
            loaded.count {
                it.category == ChatCategory.Space || it.category == ChatCategory.Mail
            }
        homeEmptyHint =
            when {
                connectivity < DcContext.DC_CONNECTIVITY_WORKING ->
                    "Could not sync your chats. Check your connection and account settings."
                homeCount == 0 && channelRows.isNotEmpty() ->
                    "No chats yet. Broadcast channels and mailing lists appear in the story row above."
                else -> "No chats yet. Your spaces and mail appear here."
            }
    }

    override fun onCleared() {
        inboxObserver.close()
        super.onCleared()
    }
}
