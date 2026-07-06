package com.polli.android.stories

import com.b44t.messenger.DcChat
import com.b44t.messenger.DcContact
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcMsg
import com.polli.domain.model.chat.MessageQuote

/** Private replies from channel stories — mirrors Delta's "Reply Privately" on broadcast posts. */
object ChannelStoryReply {

    /** Contact that should receive a private reply (channel owner / post author). */
    fun authorContactId(dc: DcContext, chat: DcChat, post: DcMsg?): Int? {
        if (chat.isOutBroadcast) return null

        val fromPost = post?.takeIf { it.isOk }?.fromId ?: 0
        if (fromPost > DcContact.DC_CONTACT_ID_LAST_SPECIAL &&
            fromPost != DcContact.DC_CONTACT_ID_SELF
        ) {
            return fromPost
        }

        // In-broadcast member lists are subscribers, not the owner — need a post to infer author.
        if (chat.isInBroadcast || chat.isMailingList) return null

        val contacts = dc.getChatContacts(chat.id) ?: return null
        return contacts.firstOrNull { id ->
            id > DcContact.DC_CONTACT_ID_LAST_SPECIAL && id != DcContact.DC_CONTACT_ID_SELF
        }
    }

    fun canReply(dc: DcContext, chat: DcChat, post: DcMsg?): Boolean =
        authorContactId(dc, chat, post) != null

    fun toComposerQuote(dc: DcContext, chat: DcChat, post: DcMsg?): MessageQuote? {
        val msg = post?.takeIf { it.isOk } ?: return null
        val text = msg.text?.trim().orEmpty().ifBlank {
            if (msg.hasFile()) {
                msg.filename?.trim().orEmpty().ifBlank { "[attachment]" }
            } else {
                return null
            }
        }
        val fromId = msg.fromId
        val authorName = when {
            fromId == DcContact.DC_CONTACT_ID_SELF -> "You"
            fromId > 0 -> dc.getContact(fromId).displayName ?: chat.name
            else -> chat.name
        }
        val color = if (fromId > 0) dc.getContact(fromId).color else 0
        val seed = if (fromId > 0) {
            dc.getContact(fromId).addr?.trim().orEmpty().ifBlank { fromId.toString() }
        } else {
            chat.name
        }
        return MessageQuote(
            msgId = msg.id,
            text = text,
            authorId = fromId,
            authorName = authorName,
            dcColorRgb = color.takeIf { it != 0 },
            authorColorSeed = seed,
        )
    }

    fun sendPrivateReply(dc: DcContext, chat: DcChat, post: DcMsg?, text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false

        val contactId = authorContactId(dc, chat, post) ?: return false
        val privateChatId = dc.createChatByContactId(contactId)
        if (privateChatId <= 0) return false

        val privateChat = dc.getChat(privateChatId)
        if (privateChat.isContactRequest) {
            dc.acceptChat(privateChatId)
        }

        val msg = DcMsg(dc, DcMsg.DC_MSG_TEXT)
        msg.setText(trimmed)
        post?.takeIf { it.isOk }?.let { quoted -> msg.setQuote(quoted) }
        return dc.sendMsg(privateChatId, msg) != 0
    }
}
