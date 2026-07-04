package com.polli.core.chat

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatSummaryFormatTest {
    @Test
    fun bothLinesUsesAuthorAndBody() {
        val preview = ChatSummaryFormat.preview("Alice", "Hello")
        assertEquals("Hello", preview.text)
        assertEquals("Alice", preview.author)
    }

    @Test
    fun singleLineHasNoAuthor() {
        val preview = ChatSummaryFormat.preview(null, "Only body")
        assertEquals("Only body", preview.text)
        assertEquals(null, preview.author)
    }
}
