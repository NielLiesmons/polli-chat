package com.polli.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveLinkRulesTest {
    @Test
    fun hiddenWhenNoArchivedChats() {
        assertEquals(
            ArchiveLinkState(visible = false, unreadCount = 0),
            ArchiveLinkRules.linkState(archivedChatCount = 0, freshOnArchivedLink = 3),
        )
    }

    @Test
    fun showsUnreadWhenArchivedExist() {
        assertEquals(
            ArchiveLinkState(visible = true, unreadCount = 5),
            ArchiveLinkRules.linkState(archivedChatCount = 2, freshOnArchivedLink = 5),
        )
    }
}
