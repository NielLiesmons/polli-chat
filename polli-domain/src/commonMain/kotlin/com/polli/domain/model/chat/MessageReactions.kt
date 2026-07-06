package com.polli.domain.model.chat

data class ReactionReactor(
    val contactId: Int,
    val name: String,
)

data class MessageReaction(
    val emoji: String,
    val count: Int,
    val reactors: List<ReactionReactor>,
)
