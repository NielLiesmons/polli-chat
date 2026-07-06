package com.polli.core.chat

/** Chatmail chatlist flags — mirrors engine constants used by inbox/archive queries. */
object ChatListFlags {
    const val ARCHIVED_ONLY = 0x01
    const val NO_SPECIALS = 0x02
    const val ADD_ALLDONE_HINT = 0x04
    const val FOR_FORWARDING = 0x08

    /** Non-archive inbox — same flags as legacy ConversationListFragment. */
    const val INBOX = ADD_ALLDONE_HINT

    /** Last reserved special chat id; rows at or below this are skipped in Polli lists. */
    const val LAST_SPECIAL_CHAT_ID = 9
}
