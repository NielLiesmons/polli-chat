package com.polli.core.chat

/**
 * Maps Delta Chat chat types to Polli home categories.
 * Primary implementation: [com.polli.android.bridge.ChatListMapper.categorize].
 */
object ChatCategorizer {
    const val DC_CHAT_TYPE_UNDEFINED = 0
    const val DC_CHAT_TYPE_SINGLE = 100
    const val DC_CHAT_TYPE_GROUP = 120
    const val DC_CHAT_TYPE_MAILINGLIST = 140
    const val DC_CHAT_TYPE_OUT_BROADCAST = 160
    const val DC_CHAT_TYPE_IN_BROADCAST = 165
}
