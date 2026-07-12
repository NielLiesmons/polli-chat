package com.polli.domain.model.chat

/** Mirrors [com.b44t.messenger.DcMsg.DC_MSG_ID_DAYMARKER] — feed day separators from the engine. */
const val MSG_ID_DAYMARKER = 9

data class MessageQuote(
    val msgId: Int,
    val text: String,
    val authorId: Int,
    val authorName: String,
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
    val authorColorSeed: String,
    val quote: MessageQuote?,
    val hasAttachment: Boolean,
    val isInfo: Boolean,
    val isEdited: Boolean = false,
    val outgoingState: OutgoingState? = null,
    val viewType: String = "Text",
    val fileName: String? = null,
    val filePath: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Int? = null,
    val savedMessageId: Int = 0,
) {
    val authorKey: String
        get() =
            authorColorSeed.ifBlank {
                if (authorId != 0) authorId.toString() else authorName
            }
}

data class MessageStub(
    val id: Int,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val authorId: Int,
    val authorName: String,
    val authorColorSeed: String,
    val isEdited: Boolean,
    val isInfo: Boolean,
    val hasText: Boolean,
    val hasAttachment: Boolean,
) {
    val authorKey: String
        get() =
            authorColorSeed.ifBlank {
                if (authorId != 0) authorId.toString() else authorName
            }

    val isDisplayable: Boolean
        get() = !isInfo && (hasText || hasAttachment)
}

sealed class FeedItem {
    data class DayMarker(val label: String, val dayKey: Long) : FeedItem()

    data object NewMessages : FeedItem()

    data class Message(
        val msgId: Int,
        val olderMsgId: Int? = null,
        val newerMsgId: Int? = null,
    ) : FeedItem()
}

fun FeedItem.stableRowId(): Long =
    when (this) {
        is FeedItem.DayMarker -> -(dayKey + 1_000_000L)
        FeedItem.NewMessages -> -2L
        is FeedItem.Message -> msgId.toLong()
    }

fun List<FeedItem>.messageCount(): Int = count { it is FeedItem.Message }

fun List<FeedItem>.displayIndexForMessage(msgId: Int): Int {
    val chron = indexOfFirst { it is FeedItem.Message && it.msgId == msgId }
    return if (chron < 0) -1 else size - 1 - chron
}

fun List<FeedItem>.messageIdAtDisplayIndex(displayIndex: Int): Int? {
    val chron = size - 1 - displayIndex
    return (getOrNull(chron) as? FeedItem.Message)?.msgId
}
