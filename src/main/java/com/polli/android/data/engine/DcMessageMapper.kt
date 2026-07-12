package com.polli.android.data.engine

import com.b44t.messenger.DcContact
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcMsg
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.MessageQuote
import com.polli.domain.model.chat.MessageStub
import com.polli.domain.model.chat.OutgoingState

/** Maps [DcMsg] (JNI) to shared domain models — same fields as [com.polli.engine.rpc.RpcMessageMapper]. */
internal object DcMessageMapper {
    fun toStub(dc: DcContext, msg: DcMsg): MessageStub? {
        if (!msg.isOk) return null
        val fromId = msg.fromId
        val authorName = authorName(dc, msg, fromId)
        val stub =
            MessageStub(
                id = msg.id,
                timestamp = normalizeTimestamp(msg.timestamp),
                isOutgoing = msg.isOutgoing,
                authorId = fromId,
                authorName = authorName,
                authorColorSeed = authorColorSeed(dc, fromId, authorName),
                isEdited = msg.isEdited,
                isInfo = msg.isInfo,
                hasText = msg.text?.trim().orEmpty().isNotEmpty(),
                hasAttachment = msg.hasFile(),
            )
        return stub.takeIf { it.isDisplayable }
    }

    fun toChatMessage(dc: DcContext, msg: DcMsg): ChatMessage? {
        if (!msg.isOk) return null
        val fromId = msg.fromId
        val authorName = authorName(dc, msg, fromId)
        return ChatMessage(
            id = msg.id,
            text = msg.text?.trim().orEmpty(),
            timestamp = normalizeTimestamp(msg.timestamp),
            isOutgoing = msg.isOutgoing,
            authorId = fromId,
            authorName = authorName,
            authorColorSeed = authorColorSeed(dc, fromId, authorName),
            quote = buildQuote(dc, msg),
            hasAttachment = msg.hasFile(),
            isInfo = msg.isInfo,
            isEdited = msg.isEdited,
            outgoingState = if (msg.isOutgoing) outgoingState(msg) else null,
            viewType = viewTypeName(msg.type),
            fileName = msg.filename,
            filePath = msg.file?.takeIf { it.isNotBlank() },
            width = msg.getWidth(0).takeIf { it > 0 },
            height = msg.getHeight(0).takeIf { it > 0 },
            durationMs = msg.duration.takeIf { it > 0 },
            savedMessageId = msg.savedMsgId,
        )
    }

    fun stubFromMessage(message: ChatMessage): MessageStub =
        MessageStub(
            id = message.id,
            timestamp = message.timestamp,
            isOutgoing = message.isOutgoing,
            authorId = message.authorId,
            authorName = message.authorName,
            authorColorSeed = message.authorColorSeed,
            isEdited = message.isEdited,
            isInfo = message.isInfo,
            hasText = message.text.isNotEmpty(),
            hasAttachment = message.hasAttachment,
        )

    private fun authorName(dc: DcContext, msg: DcMsg, fromId: Int): String =
        when {
            msg.isOutgoing -> "You"
            fromId == DcContact.DC_CONTACT_ID_SELF -> "You"
            else -> dc.getContact(fromId).displayName?.takeIf { it.isNotBlank() } ?: "Unknown"
        }

    private fun authorColorSeed(dc: DcContext, fromId: Int, authorName: String): String {
        if (fromId == 0) return authorName
        val addr = dc.getContact(fromId).addr?.trim().orEmpty()
        return addr.ifBlank { fromId.toString() }
    }

    private fun buildQuote(dc: DcContext, msg: DcMsg): MessageQuote? {
        val quoted = msg.quotedMsg?.takeIf { it.isOk } ?: return null
        val text = quoted.text?.trim().orEmpty()
        if (text.isEmpty()) return null
        val qFrom = quoted.fromId
        val qName =
            when {
                quoted.isOutgoing -> "You"
                qFrom == DcContact.DC_CONTACT_ID_SELF -> "You"
                else -> dc.getContact(qFrom).displayName?.takeIf { it.isNotBlank() } ?: "Unknown"
            }
        val color = if (qFrom > 0) dc.getContact(qFrom).color else 0
        return MessageQuote(
            msgId = quoted.id,
            text = text,
            authorId = qFrom,
            authorName = qName,
            dcColorRgb = color.takeIf { it != 0 },
            authorColorSeed = authorColorSeed(dc, qFrom, qName),
        )
    }

    private fun outgoingState(msg: DcMsg): OutgoingState =
        when {
            msg.isFailed -> OutgoingState.Failed
            msg.isRemoteRead -> OutgoingState.Read
            msg.isPending || msg.isPreparing -> OutgoingState.Sending
            msg.isDelivered -> OutgoingState.Sent
            else -> OutgoingState.Sent
        }

    private fun viewTypeName(type: Int): String =
        when (type) {
            DcMsg.DC_MSG_TEXT -> "Text"
            DcMsg.DC_MSG_IMAGE -> "Image"
            DcMsg.DC_MSG_GIF -> "Gif"
            DcMsg.DC_MSG_STICKER -> "Sticker"
            DcMsg.DC_MSG_AUDIO -> "Audio"
            DcMsg.DC_MSG_VOICE -> "Voice"
            DcMsg.DC_MSG_VIDEO -> "Video"
            DcMsg.DC_MSG_FILE -> "File"
            DcMsg.DC_MSG_CALL -> "Call"
            DcMsg.DC_MSG_WEBXDC -> "Webxdc"
            DcMsg.DC_MSG_VCARD -> "Vcard"
            else -> "Text"
        }

    private fun normalizeTimestamp(ts: Long): Long =
        if (ts > 1_000_000_000_000L) ts / 1000 else ts
}
