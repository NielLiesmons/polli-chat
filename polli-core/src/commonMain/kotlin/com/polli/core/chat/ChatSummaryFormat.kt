package com.polli.core.chat

data class ChatSummaryPreview(
    val text: String,
    val author: String?,
)

/** Inbox row preview layout: optional author prefix plus body text. */
object ChatSummaryFormat {
    fun preview(text1: String?, text2: String?): ChatSummaryPreview {
        val t1 = text1?.trim().orEmpty()
        val t2 = text2?.trim().orEmpty()
        return when {
            t1.isNotEmpty() && t2.isNotEmpty() -> ChatSummaryPreview(text = t2, author = t1)
            t2.isNotEmpty() -> ChatSummaryPreview(text = t2, author = null)
            t1.isNotEmpty() -> ChatSummaryPreview(text = t1, author = null)
            else -> ChatSummaryPreview(text = "", author = null)
        }
    }
}
