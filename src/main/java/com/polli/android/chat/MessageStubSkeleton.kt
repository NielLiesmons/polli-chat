package com.polli.android.chat

import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.MessageStub

/** Lightweight placeholder while the full [ChatMessage] hydrates from RPC. */
internal fun MessageStub.toSkeletonChatMessage(): ChatMessage =
    ChatMessage(
        id = id,
        text = "",
        timestamp = timestamp,
        isOutgoing = isOutgoing,
        authorId = authorId,
        authorName = authorName,
        authorColorSeed = authorColorSeed,
        quote = null,
        hasAttachment = hasAttachment,
        isInfo = isInfo,
        isEdited = isEdited,
    )
