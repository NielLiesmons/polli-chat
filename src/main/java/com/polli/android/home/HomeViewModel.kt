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
import org.thoughtcrime.securesms.connect.DcHelper

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository = PolliRepositories.chat(application)

    var items by mutableStateOf<List<InboxItem>>(emptyList())
        private set
    var channels by mutableStateOf<List<InboxItem>>(emptyList())
        private set
    var spacesEmptyHint by mutableStateOf("No spaces yet. Your group chats appear here.")
        private set
    var mailEmptyHint by mutableStateOf("No mail chats yet.")
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

    override fun onCleared() {
        inboxObserver.close()
        super.onCleared()
    }
}
