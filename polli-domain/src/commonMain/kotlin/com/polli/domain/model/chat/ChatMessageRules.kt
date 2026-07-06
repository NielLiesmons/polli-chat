package com.polli.domain.model.chat

/** Message action visibility rules (formerly legacy ConversationFragment). */
object ChatMessageRules {
    fun canReplyToMsg(msg: ChatMessage): Boolean = !msg.isInfo

    fun canEditMsg(msg: ChatMessage): Boolean =
        msg.isOutgoing &&
            !msg.isInfo &&
            msg.viewType != "Call" &&
            msg.text.isNotBlank()
}
