package com.polli.android.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.thoughtcrime.securesms.R
import java.util.concurrent.TimeUnit

/** Mute-duration picker — Kotlin replacement for the legacy Java MuteDialog. */
object MuteDurationDialog {
    /** Invokes [onMuted] with a duration in seconds (`-1` = forever). */
    fun show(context: Context, onMuted: (Long) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(R.string.menu_mute)
            .setNegativeButton(R.string.cancel, null)
            .setItems(R.array.mute_durations) { _, which ->
                // See https://c.delta.chat/classdc__context__t.html#a6460395925d49d2053bc95224bf5ce37
                val muteUntil = when (which) {
                    0 -> TimeUnit.HOURS.toSeconds(1)
                    1 -> TimeUnit.HOURS.toSeconds(8)
                    2 -> TimeUnit.DAYS.toSeconds(1)
                    3 -> TimeUnit.DAYS.toSeconds(7)
                    4 -> -1L // mute forever
                    else -> 0L
                }
                onMuted(muteUntil)
            }
            .show()
    }
}
