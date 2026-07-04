package com.polli.domain.navigation

/** Intent extra keys shared across Polli navigation — decoupled from legacy Java activity classes. */
object ChatIntentExtras {
    const val CHAT_ID = "chat_id"
    const val ACCOUNT_ID = "account_id"
    const val DRAFT_TEXT = "draft_text"
    const val STARTING_POSITION = "starting_position"
    const val FROM_ARCHIVED = "from_archived"
    const val FROM_WELCOME = "from_welcome"
    const val FROM_WELCOME_RAW_QR = "from_welcome_raw_qr"
    const val CLEAR_NOTIFICATIONS = "clear_notifications"
}
