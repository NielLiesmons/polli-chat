package com.polli.android.chat

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.polli.android.icons.PolliIconName
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.OutgoingState

internal object ViewOutgoingMeta {
    fun bind(
        row: LinearLayout,
        editedView: TextView,
        timeView: TextView,
        stateText: TextView,
        checksHost: FrameLayout,
        check1: ImageView,
        check2: ImageView,
        message: ChatMessage,
    ) {
        editedView.visibility = if (message.isEdited) View.VISIBLE else View.GONE
        editedView.text = "Edited"
        editedView.setTextColor(ViewChatUi.whiteAlpha(0.33f))
        timeView.text = ViewChatUi.formatTime(message.timestamp)
        timeView.setTextColor(ViewChatUi.whiteAlpha(0.66f))
        stateText.visibility = View.GONE
        checksHost.visibility = View.GONE
        check1.setImageResource(PolliIconName.Check.resId)
        check2.setImageResource(PolliIconName.Check.resId)
        val checkColor = ViewChatUi.whiteAlpha(0.66f)
        check1.setColorFilter(checkColor)
        check2.setColorFilter(checkColor)

        when (message.outgoingState) {
            OutgoingState.Sending -> {
                stateText.text = "…"
                stateText.setTextColor(ViewChatUi.whiteAlpha(0.66f))
                stateText.visibility = View.VISIBLE
            }
            OutgoingState.Sent -> {
                checksHost.visibility = View.VISIBLE
                check1.visibility = View.VISIBLE
                check2.visibility = View.GONE
            }
            OutgoingState.Read -> {
                checksHost.visibility = View.VISIBLE
                check1.visibility = View.VISIBLE
                check2.visibility = View.VISIBLE
            }
            OutgoingState.Failed -> {
                stateText.text = "!"
                stateText.setTextColor(0xFFFF4444.toInt())
                stateText.visibility = View.VISIBLE
            }
            null -> Unit
        }
    }
}
