package com.polli.domain.model.chat

/** Per-chat metadata for composer and message actions (from engine chat record). */
data class ChatSessionInfo(
    val chatId: Int,
    val name: String,
    val canSend: Boolean,
    val isEncrypted: Boolean,
    val isMultiUser: Boolean,
    val isSelfTalk: Boolean,
    val isOutBroadcast: Boolean,
    val isInBroadcast: Boolean,
) {
    val isBroadcast: Boolean get() = isOutBroadcast || isInBroadcast

    fun toActionContext(): ChatActionContext =
        ChatActionContext(
            canSend = canSend,
            isEncrypted = isEncrypted,
            isMultiUser = isMultiUser,
            isSelfTalk = isSelfTalk,
        )
}

/** Flags for message action visibility. */
data class ChatActionContext(
    val canSend: Boolean,
    val isEncrypted: Boolean,
    val isMultiUser: Boolean,
    val isSelfTalk: Boolean,
)
