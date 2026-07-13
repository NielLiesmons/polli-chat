package com.polli.android.chat

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.roundToInt

/** DC conversation_item_header — centered day pill for [StickyHeaderDecoration]. */
class ChatDayHeaderView(context: Context) : FrameLayout(context) {
    private val label = TextView(context)

    init {
        label.setTextColor(Color.argb(217, 255, 255, 255))
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        label.gravity = Gravity.CENTER
        label.setPadding(dp(12), dp(4), dp(12), dp(4))
        label.background =
            GradientDrawable().apply {
                setColor(Color.argb(38, 255, 255, 255))
                cornerRadius = dp(12).toFloat()
            }
        addView(
            label,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            },
        )
        setPadding(0, dp(8), 0, dp(4))
    }

    fun setLabel(text: CharSequence) {
        label.text = text
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
