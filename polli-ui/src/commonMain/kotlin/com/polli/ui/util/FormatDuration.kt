package com.polli.ui.util

/** Remaining/elapsed time for voice bubbles — e.g. `1:23`, `12:34`. */
fun formatAudioDurationMs(ms: Long): String {
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
