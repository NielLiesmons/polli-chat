package com.polli.android.chat

import java.text.BreakIterator
import java.util.Locale

/** Emoji-only short message detection — parity with polli web [is_emoji_only_short_text]. */
object EmojiText {
    const val EMOJI_MAGNIFY = 2.5f
    private const val MAX_SHORT_EMOJI = 3

    fun isEmojiOnlyShortText(text: String): Boolean {
        val count = countEmojiOnlyGraphemes(text) ?: return false
        return count in 1..MAX_SHORT_EMOJI
    }

    /** Grapheme count when [text] is only emoji; null if any other characters are present. */
    fun countEmojiOnlyGraphemes(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.any { it.isWhitespace() }) return null

        val iterator = BreakIterator.getCharacterInstance(Locale.getDefault())
        iterator.setText(trimmed)

        var count = 0
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val grapheme = trimmed.substring(start, end)
            if (!graphemeIsEmojiCluster(grapheme)) return null
            count++
            start = end
            end = iterator.next()
        }
        return count
    }

    private fun graphemeIsEmojiCluster(grapheme: String): Boolean {
        val codePoints = grapheme.codePoints().toArray()
        if (codePoints.isEmpty()) return false
        return codePoints.all { isEmojiCodePoint(it) || isEmojiModifier(it) }
    }

    private fun isEmojiModifier(codePoint: Int): Boolean =
        codePoint == 0xFE0F || codePoint == 0xFE0E || codePoint == 0x200D || codePoint == 0x20E3 ||
            codePoint in 0x1F3FB..0x1F3FF

    @Suppress("MagicNumber")
    private fun isEmojiCodePoint(codePoint: Int): Boolean = when (codePoint) {
        in 0x1F300..0x1FAFF,
        in 0x2600..0x27BF,
        in 0x2300..0x23FF,
        in 0x2B50..0x2B55,
        in 0x1F1E6..0x1F1FF,
        in 0x1F900..0x1F9FF,
        in 0x1FA70..0x1FAFF,
        0x00A9, 0x00AE, 0x203C, 0x2049, 0x2122, 0x2139,
        in 0x2194..0x2199, 0x21A9, 0x21AA, 0x231A, 0x231B, 0x2328, 0x23CF,
        in 0x23E9..0x23F3, in 0x23F8..0x23FA, 0x24C2, 0x25AA, 0x25AB, 0x25B6, 0x25C0,
        in 0x25FB..0x25FE, 0x2614, 0x2615, in 0x2648..0x2653, 0x267F, 0x2693, 0x26A1,
        0x26AA, 0x26AB, 0x26BD, 0x26BE, 0x26C4, 0x26C5, 0x26CE, 0x26D4, 0x26EA, 0x26F2,
        0x26F3, 0x26F5, 0x26FA, 0x26FD, 0x2702, 0x2705, in 0x2708..0x270D, 0x270F,
        0x2712, 0x2714, 0x2716, 0x271D, 0x2721, 0x2728, 0x2733, 0x2734, 0x2744, 0x2747,
        0x274C, 0x274E, in 0x2753..0x2755, 0x2757, 0x2763, 0x2764, in 0x2795..0x2797,
        0x27A1, 0x27B0, 0x27BF, 0x2934, 0x2935, in 0x2B05..0x2B07, 0x2B1B, 0x2B1C,
        0x3030, 0x303D, 0x3297, 0x3299,
        -> true
        else -> false
    }
}
