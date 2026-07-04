package com.polli.core.chat

/** Platform-neutral chat metadata for inbox categorization. */
data class ChatKind(
    val type: Int,
    val isDeviceTalk: Boolean,
    val isSelfTalk: Boolean,
    val isOutBroadcast: Boolean,
    val isInBroadcast: Boolean,
    val isMailingList: Boolean,
    val isMultiUser: Boolean,
)
