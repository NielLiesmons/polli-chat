package com.polli.core.chat

/**
 * Maps Chatmail engine chat metadata to Polli home categories.
 * Platform JNI / RPC types are adapted to [ChatKind] at the boundary.
 *
 * Source of truth: `jni/deltachat-core-rust/polli-home` (Rust). Keep in sync.
 */
object ChatCategorizer {
    const val CHAT_TYPE_UNDEFINED = 0
    const val CHAT_TYPE_SINGLE = 100
    const val CHAT_TYPE_GROUP = 120
    const val CHAT_TYPE_MAILINGLIST = 140
    const val CHAT_TYPE_OUT_BROADCAST = 160
    const val CHAT_TYPE_IN_BROADCAST = 165

    /**
     * Polli tab routing:
     * - Broadcasts / mailing lists → story row (Channel)
     * - Multi-user groups → Spaces
     * - Everything else (1:1, contact requests, …) → Mail
     */
    fun categorize(chat: ChatKind): ChatCategory {
        if (chat.isDeviceTalk || chat.isSelfTalk) {
            return ChatCategory.Skip
        }
        if (chat.isOutBroadcast || chat.isInBroadcast || chat.isMailingList) {
            return ChatCategory.Channel
        }
        return when {
            chat.type == CHAT_TYPE_GROUP -> ChatCategory.Space
            chat.type == CHAT_TYPE_SINGLE -> ChatCategory.Mail
            chat.isMultiUser -> ChatCategory.Space
            else -> ChatCategory.Mail
        }
    }
}
