package com.polli.android.chat

import android.content.Context
import chat.delta.rpc.RpcException
import com.b44t.messenger.DcContact
import com.b44t.messenger.DcContext
import org.thoughtcrime.securesms.connect.DcHelper
import java.util.Collections

data class ReactionReactor(
    val contactId: Int,
    val name: String,
)

data class BubbleReaction(
    val emoji: String,
    val count: Int,
    val reactors: List<ReactionReactor>,
)

object MessageReactions {
    val DEFAULT_EMOJI = listOf(
        "👍", "❤️", "😂", "😮", "😢", "🙏", "👎", "🎉", "🔥", "💯", "😍", "🤔",
    )

    fun sendReaction(context: Context, msgId: Int, emoji: String?) {
        val rpc = DcHelper.getRpc(context)
        val accId = DcHelper.getContext(context).accountId
        try {
            if (emoji.isNullOrBlank()) {
                rpc.sendReaction(accId, msgId, Collections.singletonList(""))
            } else {
                rpc.sendReaction(accId, msgId, Collections.singletonList(emoji))
            }
        } catch (_: RpcException) {
        }
    }

    fun loadReactionSummary(context: Context, msgId: Int): List<BubbleReaction> {
        return try {
            val dc = DcHelper.getContext(context)
            val rpc = DcHelper.getRpc(context)
            val summary = rpc.getMessageReactions(dc.accountId, msgId) ?: return emptyList()
            val byEmoji = linkedMapOf<String, MutableList<Int>>()

            summary.reactionsByContact?.forEach { (contactKey, emojis) ->
                val contactId = contactKey.toIntOrNull() ?: return@forEach
                emojis?.forEach { emoji ->
                    if (!emoji.isNullOrBlank()) {
                        byEmoji.getOrPut(emoji) { mutableListOf() }.add(contactId)
                    }
                }
            }

            summary.reactions
                ?.mapNotNull { reaction ->
                    val emoji = reaction.emoji ?: return@mapNotNull null
                    val count = reaction.count ?: 1
                    val contactIds = byEmoji[emoji].orEmpty()
                    val reactors = contactIds.take(3).map { contactId ->
                        reactorFor(dc, contactId)
                    }
                    BubbleReaction(emoji = emoji, count = count, reactors = reactors)
                }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun reactorFor(dc: DcContext, contactId: Int): ReactionReactor {
        val name = when (contactId) {
            DcContact.DC_CONTACT_ID_SELF -> "You"
            else -> {
                val contact = dc.getContact(contactId)
                contact.displayName?.takeIf { it.isNotBlank() } ?: "Unknown"
            }
        }
        return ReactionReactor(contactId = contactId, name = name)
    }
}
