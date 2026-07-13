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
import com.polli.ui.theme.PolliDimens
import kotlin.math.roundToInt

/**
 * Fast View bubble row for plain text messages.
 *
 * This is intentionally DC-style: mutate existing views in bind() and do no Compose work.
 * Styling can be refined later; the goal here is perf parity first.
 */
class PolliTextMessageRowView(context: Context) : FrameLayout(context) {
    private val bubble = TextView(context)
    private val rowPadHPx = dp(PolliDimens.ChatRowPaddingH.value)
    private val outgoingBg =
        GradientDrawable().apply {
            colors = intArrayOf(0xFF6B4EFF.toInt(), 0xFF9B59FF.toInt())
            orientation = GradientDrawable.Orientation.BL_TR
        }
    private val incomingBg =
        GradientDrawable().apply {
            setColor(0xFF666666.toInt())
        }

    init {
        bubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
        bubble.setLineSpacing(0f, 1.15f)
        bubble.setPadding(
            dp(PolliDimens.ChatBubbleInsetH.value),
            dp((PolliDimens.ChatBubblePaddingV + PolliDimens.ChatBubbleTextOnlyExtraTop).value),
            dp(PolliDimens.ChatBubbleInsetH.value),
            dp(PolliDimens.ChatBubblePaddingV.value),
        )
        addView(
            bubble,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            },
        )
        setPadding(rowPadHPx, dp(PolliDimens.ChatRowTopCollapsed.value), rowPadHPx, dp(0f))
    }

    fun bind(
        message: ChatMessage,
        layout: MessageGroupLayout,
        maxBubbleWidthPx: Int,
        highlighted: Boolean,
    ) {
        val topPad =
            if (layout.isFirstInStack) dp(PolliDimens.ChatRowTop.value) else dp(PolliDimens.ChatRowTopCollapsed.value)
        setPadding(rowPadHPx, topPad, rowPadHPx, paddingBottom)

        bubble.text =
            when {
                message.isInfo -> "[Info]"
                message.hasAttachment && message.text.isNotBlank() -> message.text
                message.hasAttachment -> "[${message.viewType}]"
                message.text.isNotBlank() -> message.text
                else -> "[${message.viewType}]"
            }
        bubble.maxWidth = maxBubbleWidthPx
        bubble.background =
            if (message.isOutgoing) {
                outgoingBg.apply {
                    setCornerRadii(bubbleCornerRadii(outgoing = true, isLastInStack = layout.isLastInStack))
                }
            } else {
                incomingBg.apply {
                    setCornerRadii(bubbleCornerRadii(outgoing = false, isLastInStack = layout.isLastInStack))
                }
            }
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

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private fun bubbleCornerRadii(outgoing: Boolean, isLastInStack: Boolean): FloatArray {
        val full = dp(PolliDimens.ChatBubbleRadius.value).toFloat()
        val tail = dp(PolliDimens.ChatBubbleTailRadius.value).toFloat()
        // Radii order: TL, TR, BR, BL (each pair x,y)
        return if (outgoing) {
            if (isLastInStack) {
                floatArrayOf(full, full, full, full, tail, tail, full, full)
            } else {
                floatArrayOf(full, full, full, full, full, full, full, full)
            }
        } else {
            if (isLastInStack) {
                floatArrayOf(full, full, full, full, full, full, tail, tail)
            } else {
                floatArrayOf(full, full, full, full, full, full, full, full)
            }
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()
}

