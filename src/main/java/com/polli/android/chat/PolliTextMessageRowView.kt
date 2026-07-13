package com.polli.android.chat

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.polli.core.chat.MessageGroupLayout
import com.polli.domain.model.chat.ChatMessage
import kotlin.math.roundToInt

/**
 * Fast View bubble row for plain text messages.
 *
 * This is intentionally DC-style: mutate existing views in bind() and do no Compose work.
 * Styling can be refined later; the goal here is perf parity first.
 */
class PolliTextMessageRowView(context: Context) : FrameLayout(context) {
    private val bubble = TextView(context)
    private val rowPadPx = dp(12)
    private val outgoingBg =
        GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            colors = intArrayOf(0xFF6B4EFF.toInt(), 0xFF9B59FF.toInt())
            orientation = GradientDrawable.Orientation.BL_TR
        }
    private val incomingBg =
        GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(0xFF666666.toInt())
        }

    init {
        bubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        bubble.setLineSpacing(0f, 1.15f)
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10))
        addView(
            bubble,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            },
        )
        setPadding(rowPadPx, dp(2), rowPadPx, dp(2))
    }

    fun bind(
        message: ChatMessage,
        layout: MessageGroupLayout,
        maxBubbleWidthPx: Int,
        highlighted: Boolean,
    ) {
        val topPad = if (layout.isFirstInStack) dp(6) else dp(1)
        setPadding(paddingLeft, topPad, paddingRight, paddingBottom)

        bubble.text = message.text
        bubble.maxWidth = maxBubbleWidthPx
        bubble.background = if (message.isOutgoing) outgoingBg else incomingBg
        bubble.setTextColor(0xFFFFFFFF.toInt())
        bubble.typeface = if (message.isEdited) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        bubble.alpha = if (highlighted) 1f else 0.98f

        (bubble.layoutParams as LayoutParams).gravity =
            if (message.isOutgoing) {
                Gravity.END or Gravity.CENTER_VERTICAL
            } else {
                Gravity.START or Gravity.CENTER_VERTICAL
            }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()
}

