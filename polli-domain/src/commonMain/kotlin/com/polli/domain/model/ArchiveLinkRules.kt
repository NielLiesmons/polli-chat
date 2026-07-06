package com.polli.domain.model

/**
 * Archive link row rules — mirror `polli-home::archive_link_state`.
 */
object ArchiveLinkRules {
    const val ARCHIVED_LINK_CHAT_ID = 6

    fun linkState(archivedChatCount: Int, freshOnArchivedLink: Int): ArchiveLinkState {
        val visible = archivedChatCount > 0
        return ArchiveLinkState(
            visible = visible,
            unreadCount = if (visible) freshOnArchivedLink.coerceAtLeast(0) else 0,
        )
    }
}
