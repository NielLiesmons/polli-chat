package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException
import chat.delta.rpc.types.Message
import chat.delta.rpc.types.MessageData
import chat.delta.rpc.types.Viewtype
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.MessageQuote
import com.polli.domain.model.chat.MessageStub
import com.polli.domain.model.chat.OutgoingState

internal object RpcMessageMapper {
    // Delta Chat core message states (see DcMsg): in-states are < 18, out-states >= 18.
    private const val CONTACT_ID_SELF = 1
    private const val STATE_OUT_PREPARING = 18
    private const val STATE_OUT_PENDING = 20
    private const val STATE_OUT_FAILED = 24
    private const val STATE_OUT_DELIVERED = 26
    private const val STATE_OUT_READ = 28

    private fun isOutgoing(msg: Message): Boolean = (msg.fromId ?: 0) == CONTACT_ID_SELF

    fun toStub(msg: Message, selfName: String = "You"): MessageStub? {
        val id = msg.id ?: return null
        val fromId = msg.fromId ?: 0
        val authorName = authorName(msg, selfName)
        val stub =
            MessageStub(
                id = id,
                timestamp = normalizeTimestamp(msg.timestamp),
                isOutgoing = isOutgoing(msg),
                authorId = fromId,
                authorName = authorName,
                authorColorSeed = authorColorSeed(msg, authorName),
                isEdited = msg.isEdited == true,
                isInfo = msg.isInfo == true,
                hasText = msg.text?.trim().orEmpty().isNotEmpty(),
                hasAttachment = !msg.file.isNullOrBlank(),
            )
        return stub.takeIf { it.isDisplayable }
    }

    fun toChatMessage(msg: Message, selfName: String = "You"): ChatMessage? {
        val id = msg.id ?: return null
        val fromId = msg.fromId ?: 0
        val authorName = authorName(msg, selfName)
        val quote = buildQuote(msg)
        return ChatMessage(
            id = id,
            text = msg.text?.trim().orEmpty(),
            timestamp = normalizeTimestamp(msg.timestamp),
            isOutgoing = isOutgoing(msg),
            authorId = fromId,
            authorName = authorName,
            authorColorSeed = authorColorSeed(msg, authorName),
            quote = quote,
            hasAttachment = !msg.file.isNullOrBlank(),
            isInfo = msg.isInfo == true,
            isEdited = msg.isEdited == true,
            outgoingState = outgoingState(msg.state),
            viewType = msg.viewType?.name ?: "Text",
            fileName = msg.fileName,
            filePath = msg.file?.takeIf { it.isNotBlank() },
            width = msg.dimensionsWidth,
            height = msg.dimensionsHeight,
            durationMs = msg.duration,
            savedMessageId = msg.savedMessageId ?: 0,
        )
    }

    fun messageData(text: String, quotedMessageId: Int? = null): MessageData {
        val data = MessageData()
        data.text = text
        data.viewtype = Viewtype.Text
        if (quotedMessageId != null && quotedMessageId > 0) {
            data.quotedMessageId = quotedMessageId
        }
        return data
    }

    fun viewtypeFromName(name: String): Viewtype =
        when (name) {
            "Text" -> Viewtype.Text
            "Image" -> Viewtype.Image
            "Gif" -> Viewtype.Gif
            "Sticker" -> Viewtype.Sticker
            "Audio" -> Viewtype.Audio
            "Voice" -> Viewtype.Voice
            "Video" -> Viewtype.Video
            "File" -> Viewtype.File
            "Call" -> Viewtype.Call
            "Webxdc" -> Viewtype.Webxdc
            "Vcard" -> Viewtype.Vcard
            else -> Viewtype.Text
        }

    private fun authorName(msg: Message, selfName: String): String {
        if (isOutgoing(msg)) return selfName
        return msg.overrideSenderName?.takeIf { it.isNotBlank() }
            ?: msg.sender?.displayName?.takeIf { it.isNotBlank() }
            ?: "Unknown"
    }

    private fun authorColorSeed(msg: Message, authorName: String): String {
        val fromId = msg.fromId ?: 0
        if (fromId == 0) return authorName
        val addr = msg.sender?.address?.trim().orEmpty()
        return addr.ifBlank { fromId.toString() }
    }

    private fun buildQuote(msg: Message): MessageQuote? {
        val quote = msg.quote ?: return null
        return when (quote) {
            is chat.delta.rpc.types.MessageQuote.WithMessage -> {
                MessageQuote(
                    msgId = quote.messageId ?: 0,
                    text = quote.text.orEmpty(),
                    authorId = 0,
                    authorName = quote.authorDisplayName.orEmpty(),
                )
            }
            is chat.delta.rpc.types.MessageQuote.JustText -> {
                MessageQuote(
                    msgId = 0,
                    text = quote.text.orEmpty(),
                    authorId = 0,
                    authorName = "",
                )
            }
            else -> null
        }
    }

    private fun outgoingState(state: Int?): OutgoingState? {
        return when (state) {
            in STATE_OUT_PREPARING..STATE_OUT_PENDING -> OutgoingState.Sending
            STATE_OUT_DELIVERED -> OutgoingState.Sent
            STATE_OUT_READ -> OutgoingState.Read
            STATE_OUT_FAILED -> OutgoingState.Failed
            else -> null
        }
    }

    private fun normalizeTimestamp(ts: Int?): Long {
        val value = ts?.toLong() ?: 0L
        return if (value > 1_000_000_000_000L) value / 1000 else value
    }
}
