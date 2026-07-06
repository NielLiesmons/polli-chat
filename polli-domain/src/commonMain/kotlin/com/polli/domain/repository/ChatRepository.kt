package com.polli.domain.repository

import com.polli.core.chat.ChatCategory
import com.polli.domain.model.ArchiveLinkState
import com.polli.domain.model.InboxItem

interface ChatRepository {
    fun loadInbox(query: String? = null): List<InboxItem>
    fun loadArchived(query: String? = null): List<InboxItem>
    fun loadByCategory(category: ChatCategory, query: String? = null): List<InboxItem> =
        loadInbox(query).filter { it.category == category }

    fun archiveLinkState(): ArchiveLinkState

    fun getFreshMessageCount(chatId: Int): Int

    fun getSession(chatId: Int): com.polli.domain.model.chat.ChatSessionInfo?

    fun createChatByContactId(contactId: Int): Int?

    /** Register for inbox refresh signals; returns a handle to unregister. */
    fun observeInbox(onUpdate: () -> Unit): AutoCloseable
}
