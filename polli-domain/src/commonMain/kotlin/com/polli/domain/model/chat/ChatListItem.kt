package com.polli.domain.model.chat

/** One row from engine `get_message_list_items` — message id or day marker with timestamp. */
sealed class ChatListItem {
    data class Message(val msgId: Int) : ChatListItem()

    /** @param timestampMs unix millis from the engine day marker */
    data class DayMarker(val timestampMs: Long) : ChatListItem()
}
