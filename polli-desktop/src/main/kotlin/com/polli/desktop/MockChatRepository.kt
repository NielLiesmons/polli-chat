package com.polli.desktop

import com.polli.core.chat.ChatCategory
import com.polli.domain.model.ArchiveLinkState
import com.polli.domain.model.InboxItem
import com.polli.domain.repository.ChatRepository

/** Fake inbox data for desktop UI shell when the engine is unavailable. */
class MockChatRepository : ChatRepository {
    private val now = System.currentTimeMillis() / 1000

    private val inboxItems =
        listOf(
            InboxItem(
                chatId = 101,
                name = "Alice Chen",
                preview = "See you tomorrow at the cafe",
                previewAuthor = null,
                updatedAt = now - 120,
                unreadCount = 2,
                category = ChatCategory.Mail,
                profileImage = null,
                colorSeed = "Alice Chen",
            ),
            InboxItem(
                chatId = 102,
                name = "Polli Team",
                preview = "Desktop KMP shell is live",
                previewAuthor = "Bob",
                updatedAt = now - 3600,
                unreadCount = 0,
                category = ChatCategory.Space,
                profileImage = null,
                colorSeed = "Polli Team",
            ),
            InboxItem(
                chatId = 103,
                name = "Weekly Digest",
                preview = "Three new stories",
                previewAuthor = null,
                updatedAt = now - 7200,
                unreadCount = 5,
                category = ChatCategory.Channel,
                profileImage = null,
                colorSeed = "Weekly Digest",
            ),
            InboxItem(
                chatId = 104,
                name = "dev@chatmail.test",
                preview = "Build passed on feature branch",
                previewAuthor = null,
                updatedAt = now - 86400,
                unreadCount = 0,
                category = ChatCategory.Mail,
                profileImage = null,
                colorSeed = "dev@chatmail.test",
            ),
        )

    private val archivedItems =
        listOf(
            InboxItem(
                chatId = 201,
                name = "Old Project",
                preview = "Archived thread",
                previewAuthor = null,
                updatedAt = now - 604800,
                unreadCount = 0,
                category = ChatCategory.Space,
                profileImage = null,
                colorSeed = "Old Project",
            ),
        )

    override fun loadInbox(query: String?): List<InboxItem> = filter(inboxItems, query)

    override fun loadArchived(query: String?): List<InboxItem> = filter(archivedItems, query)

    override fun archiveLinkState(): ArchiveLinkState =
        ArchiveLinkState(visible = archivedItems.isNotEmpty(), unreadCount = 0)

    override fun getFreshMessageCount(chatId: Int): Int =
        (inboxItems + archivedItems).firstOrNull { it.chatId == chatId }?.unreadCount ?: 0

    override fun observeInbox(onUpdate: () -> Unit): AutoCloseable = AutoCloseable { }

    private fun filter(items: List<InboxItem>, query: String?): List<InboxItem> {
        val q = query?.trim()?.lowercase().orEmpty()
        if (q.isEmpty()) return items
        return items.filter {
            it.name.lowercase().contains(q) || it.preview.lowercase().contains(q)
        }
    }
}
