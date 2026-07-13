package com.polli.android.chat

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.polli.domain.model.chat.ChatMessage
import com.polli.android.theme.PolliDimens

/** DC conversation_item_update — centered info/system rows. */
class PolliInfoMessageView(context: Context) : FrameLayout(context) {
    private val label = TextView(context)

    init {
        label.gravity = Gravity.CENTER
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        label.setTextColor(ViewChatUi.whiteAlpha(0.55f))
        label.setPadding(
            ViewChatUi.dp(context, 16f),
            ViewChatUi.dp(context, 6f),
            ViewChatUi.dp(context, 16f),
            ViewChatUi.dp(context, 6f),
        )
        addView(label, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        setPadding(
            0,
            ViewChatUi.dp(context, PolliDimens.ChatRowTopCollapsed.value),
            0,
            0,
        )
    }

    fun bind(message: ChatMessage) {
        label.text = message.text.ifBlank { message.viewType }
    }
}
