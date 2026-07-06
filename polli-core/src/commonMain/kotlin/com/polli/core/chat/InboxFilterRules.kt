package com.polli.core.chat

/** Inbox row filters — mirror `polli-home::is_listable_inbox_chat`. */
object InboxFilterRules {
    const val LAST_SPECIAL_CHAT_ID = 9

    fun isListableChatId(chatId: Int): Boolean = chatId > LAST_SPECIAL_CHAT_ID
}
