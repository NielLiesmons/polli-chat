package com.polli.android.chat

import android.content.Context

/** MRU emoji for reaction row + picker — most recent first. */
object RecentEmojiStore {
    private const val PREFS = "polli_recent_emoji"
    private const val KEY = "recent"
    private const val MAX = 32

    fun record(context: Context, emoji: String) {
        val normalized = emoji.trim()
        if (normalized.isEmpty()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = load(context).toMutableList()
        current.remove(normalized)
        current.add(0, normalized)
        while (current.size > MAX) current.removeLast()
        prefs.edit().putString(KEY, current.joinToString("\u0000")).apply()
    }

    fun load(context: Context): List<String> {
        val raw =
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, null)
                ?: return emptyList()
        return raw.split('\u0000').filter { it.isNotEmpty() }
    }

    /** Recent first, then defaults without duplicates. */
    fun orderedQuickPick(context: Context): List<String> {
        val recent = load(context)
        val tail = MessageReactions.DEFAULT_EMOJI.filter { it !in recent }
        return recent + tail
    }
}
