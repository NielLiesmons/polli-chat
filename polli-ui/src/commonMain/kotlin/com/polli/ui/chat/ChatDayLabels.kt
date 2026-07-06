package com.polli.ui.chat

import java.text.DateFormat
import java.util.Date

/** Platform-neutral relative day labels for feed day markers. */
fun formatChatDayLabel(timestampSec: Long): String {
    val tsMs = timestampSec * 1000
    val now = System.currentTimeMillis()
    val dayMs = 86_400_000L
    val todayStart = now - (now % dayMs)
    return when {
        tsMs >= todayStart -> "Today"
        tsMs >= todayStart - dayMs -> "Yesterday"
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(tsMs))
    }
}
