package com.polli.core.time

/** Compact relative timestamps for inbox rows (epoch seconds). */
object RelativeTimeFormat {
    fun format(epochSec: Long, nowSec: Long): String {
        if (epochSec <= 0) return "—"
        val diff = (nowSec - epochSec).coerceAtLeast(0)
        return when {
            diff < 60 -> "now"
            diff < 3600 -> "${diff / 60}m"
            diff < 86400 -> "${diff / 3600}h"
            diff < 604800 -> "${diff / 86400}d"
            else -> "1w+"
        }
    }
}
