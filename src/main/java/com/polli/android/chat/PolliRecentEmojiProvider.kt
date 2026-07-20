package com.polli.android.chat

import android.content.Context
import androidx.emoji2.emojipicker.RecentEmojiProvider

/** Bridges [RecentEmojiStore] to androidx EmojiPickerView's recent category. */
class PolliRecentEmojiProvider(
    private val context: Context,
) : RecentEmojiProvider {
    override fun recordSelection(emoji: String) {
        RecentEmojiStore.record(context, emoji)
    }

    override suspend fun getRecentEmojiList(): List<String> = RecentEmojiStore.load(context)
}
