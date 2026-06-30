package com.polli.android.chat

import com.b44t.messenger.DcContact
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcMsg

data class MessageQuote(
    val msgId: Int,
    val text: String,
    val authorId: Int,
    val authorName: String,
    /** DC contact color as 0x00RRGGBB from [com.b44t.messenger.DcContact.getColor]. */
    val dcColorRgb: Int? = null,
    val authorColorSeed: String = "",
)

enum class OutgoingState {
    Sending,
    Sent,
    Read,
    Failed,
}

data class ChatMessage(
    val id: Int,
    val text: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val authorId: Int,
    val authorName: String,
    /** Polli hashes profile colors from contact addr when available. */
    val authorColorSeed: String,
    val quote: MessageQuote?,
    val hasAttachment: Boolean,
    val isInfo: Boolean,
    val isEdited: Boolean = false,
    val outgoingState: OutgoingState? = null,
    val viewType: Int = DcMsg.DC_MSG_TEXT,
    val fileName: String? = null,
) {
    val authorKey: String
        get() = authorColorSeed.ifBlank {
            if (authorId != 0) authorId.toString() else authorName
        }
}

private fun normalizeTimestamp(ts: Long): Long =
    if (ts > 1_000_000_000_000L) ts / 1000 else ts

private fun authorColorSeed(dcContext: DcContext, fromId: Int, authorName: String): String {
    if (fromId == 0) return authorName
    val addr = dcContext.getContact(fromId).addr?.trim().orEmpty()
    return addr.ifBlank { fromId.toString() }
}

sealed class FeedItem {
    data class DayMarker(val label: String) : FeedItem()
    /** Row identity only — grouping is derived per compose from neighbor stubs (DC bind-time pattern). */
    data class Message(val msgId: Int) : FeedItem()
}

object MessageLoader {
    fun stubFromDcMsg(dcContext: DcContext, msg: DcMsg): MessageStub {
        val fromId = msg.fromId
        val authorName = when {
            msg.isOutgoing -> "You"
            fromId == DcContact.DC_CONTACT_ID_SELF -> "You"
            else -> dcContext.getContact(fromId).displayName ?: "Unknown"
        }
        return MessageStub(
            id = msg.id,
            timestamp = normalizeTimestamp(msg.timestamp),
            isOutgoing = msg.isOutgoing,
            authorId = fromId,
            authorName = authorName,
            authorColorSeed = authorColorSeed(dcContext, fromId, authorName),
            isEdited = msg.isEdited,
            isInfo = msg.isInfo,
            hasText = msg.text?.trim().orEmpty().isNotEmpty(),
            hasAttachment = msg.hasFile(),
        )
    }

    fun fromDcMsg(dcContext: DcContext, msg: DcMsg): ChatMessage {
        val fromId = msg.fromId
        val authorName = when {
            msg.isOutgoing -> "You"
            fromId == DcContact.DC_CONTACT_ID_SELF -> "You"
            else -> dcContext.getContact(fromId).displayName ?: "Unknown"
        }
        val quote = buildQuote(dcContext, msg)
        val colorSeed = authorColorSeed(dcContext, fromId, authorName)
        return ChatMessage(
            id = msg.id,
            text = msg.text?.trim().orEmpty(),
            timestamp = normalizeTimestamp(msg.timestamp),
            isOutgoing = msg.isOutgoing,
            authorId = fromId,
            authorName = authorName,
            authorColorSeed = colorSeed,
            quote = quote,
            hasAttachment = msg.hasFile(),
            isInfo = msg.isInfo,
            isEdited = msg.isEdited,
            outgoingState = if (msg.isOutgoing) outgoingState(msg) else null,
            viewType = msg.type,
            fileName = msg.filename,
        )
    }

    private fun buildQuote(dcContext: DcContext, msg: DcMsg): MessageQuote? {
        val quoted = msg.quotedMsg?.takeIf { it.isOk } ?: return null
        val text = quoted.text?.trim().orEmpty()
        if (text.isEmpty()) return null
        val qFrom = quoted.fromId
        val qName = when {
            quoted.isOutgoing -> "You"
            qFrom == DcContact.DC_CONTACT_ID_SELF -> "You"
            else -> dcContext.getContact(qFrom).displayName ?: "Unknown"
        }
        val color = if (qFrom > 0) dcContext.getContact(qFrom).color else 0
        val qSeed = authorColorSeed(dcContext, qFrom, qName)
        return MessageQuote(
            msgId = quoted.id,
            text = text,
            authorId = qFrom,
            authorName = qName,
            dcColorRgb = color.takeIf { it != 0 },
            authorColorSeed = qSeed,
        )
    }

    private fun outgoingState(msg: DcMsg): OutgoingState = when {
        msg.isFailed -> OutgoingState.Failed
        msg.isRemoteRead -> OutgoingState.Read
        msg.isPending || msg.isPreparing -> OutgoingState.Sending
        msg.isDelivered -> OutgoingState.Sent
        else -> OutgoingState.Sent
    }

    fun buildIdFeed(ids: IntArray): List<FeedItem> {
        val items = ArrayList<FeedItem>(ids.size)
        for (id in ids) {
            if (id <= DcMsg.DC_MSG_ID_DAYMARKER) continue
            items.add(FeedItem.Message(id))
        }
        return items
    }
}
