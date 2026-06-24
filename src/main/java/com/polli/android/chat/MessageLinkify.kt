package com.polli.android.chat

/** Plain text or a detected URL segment inside a message body. */
sealed class MessagePart {
    data class Text(val value: String) : MessagePart()
    data class Link(val href: String, val label: String) : MessagePart()
}

object MessageLinkify {
    fun splitMessageParts(input: String): List<MessagePart> {
        if (input.isEmpty()) return emptyList()
        val parts = ArrayList<MessagePart>()
        var rest = input
        while (rest.isNotEmpty()) {
            val start = findEarliestUrlStart(rest)
            if (start == null) {
                parts.add(MessagePart.Text(rest))
                break
            }
            if (start > 0) {
                parts.add(MessagePart.Text(rest.substring(0, start)))
            }
            val (url, after) = extractUrl(rest.substring(start))
            if (url.isEmpty()) {
                parts.add(MessagePart.Text(rest.substring(0, 1)))
                rest = rest.substring(1)
                continue
            }
            val href = if (url.startsWith("www.")) "https://$url" else url
            parts.add(MessagePart.Link(href = href, label = url))
            rest = after
        }
        return parts
    }

    fun firstUrl(input: String): String? =
        splitMessageParts(input).firstOrNull { it is MessagePart.Link }
            ?.let { (it as MessagePart.Link).href }

    private fun findEarliestUrlStart(s: String): Int? {
        var earliest: Int? = null
        for ((pat, needsBoundary) in listOf("https://" to false, "http://" to false, "www." to true)) {
            val pos = s.indexOf(pat)
            if (pos < 0) continue
            if (needsBoundary && !isWwwBoundary(s, pos)) continue
            earliest = when (val e = earliest) {
                null -> pos
                else -> minOf(e, pos)
            }
        }
        return earliest
    }

    private fun isWwwBoundary(s: String, pos: Int): Boolean {
        if (pos == 0) return true
        val prev = s[pos - 1]
        return !prev.isLetterOrDigit() && prev != '@' && prev != '.'
    }

    private fun extractUrl(s: String): Pair<String, String> {
        var end = 0
        for (ch in s) {
            if (ch.isWhitespace() || ch in "<>\"'`") break
            end += ch.toString().length
        }
        while (end > 0) {
            val last = s[end - 1]
            if (last in ")]}.,;:!?…") {
                end--
            } else {
                break
            }
        }
        if (end == 0) return "" to s
        return s.substring(0, end) to s.substring(end)
    }
}
