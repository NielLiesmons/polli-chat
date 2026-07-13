package com.polli.android.chat

import android.content.Context
import android.widget.LinearLayout
import kotlin.math.min

/** Caps measured width — Compose [widthIn(max = …)] equivalent for View bubbles. */
internal class MaxWidthLinearLayout(context: Context) : LinearLayout(context) {
    private var maxWidthPx = Int.MAX_VALUE

    fun setMaxBubbleWidth(px: Int) {
        if (maxWidthPx == px) return
        maxWidthPx = px.coerceAtLeast(0)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        val size = MeasureSpec.getSize(widthMeasureSpec)
        val capped =
            when (mode) {
                MeasureSpec.UNSPECIFIED -> MeasureSpec.makeMeasureSpec(maxWidthPx, MeasureSpec.AT_MOST)
                else -> MeasureSpec.makeMeasureSpec(min(size, maxWidthPx), mode)
            }
        super.onMeasure(capped, heightMeasureSpec)
    }
}
