package com.polli.android.notes

/** One row in the Notes tab — backed by a self-talk [com.b44t.messenger.DcMsg]. */
data class Note(
    val msgId: Int,
    val title: String,
    val preview: String,
    val body: String,
    val timestamp: Long,
    val hasAttachment: Boolean = false,
)

object NoteText {
    private const val PREVIEW_MAX = 140

    fun parse(text: String): Pair<String, String> {
        val normalized = text.trim()
        if (normalized.isEmpty()) return "Untitled" to ""

        val lines = normalized.lines()
        val rawTitle = lines.first().trim().removePrefix("#").trim()
        val title = rawTitle.ifBlank { "Untitled" }

        val remainder = lines.drop(1).joinToString("\n").trim()
        val preview = when {
            remainder.isBlank() -> ""
            else -> {
                val firstBodyLine = remainder.lines().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                val candidate = if (firstBodyLine.isNotBlank()) {
                    firstBodyLine.removePrefix(">").trim().removePrefix("-").trim()
                } else {
                    remainder.replace('\n', ' ').trim()
                }
                candidate.take(PREVIEW_MAX).let { if (candidate.length > PREVIEW_MAX) "$it…" else it }
            }
        }
        return title to preview
    }
}
