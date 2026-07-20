package com.polli.android.chat

import android.content.Context
import android.view.View
import android.view.ViewGroup

/**
 * Incoming bubble row: height follows the bubble only. Avatar (when present) is bottom-start
 * aligned and may overflow upward — it must not inflate row height / one-liner padding.
 */
internal class IncomingMessageLane(context: Context) : ViewGroup(context) {
    var avatarSizePx: Int = 0
    var avatarGapPx: Int = 0
    var showAvatar: Boolean = false

    private val avatarOffsetPx: Int
        get() = avatarSizePx + avatarGapPx

    init {
        clipChildren = false
        clipToPadding = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val bubble = getChildAt(1) ?: run {
            setMeasuredDimension(0, 0)
            return
        }
        val avatar = getChildAt(0)

        val bubbleWidthSpec =
            MeasureSpec.makeMeasureSpec((width - avatarOffsetPx).coerceAtLeast(0), MeasureSpec.AT_MOST)
        val bubbleHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measureChild(bubble, bubbleWidthSpec, bubbleHeightSpec)

        if (showAvatar && avatar != null && avatar.visibility != View.GONE) {
            val exact = MeasureSpec.makeMeasureSpec(avatarSizePx, MeasureSpec.EXACTLY)
            avatar.measure(exact, exact)
        }

        setMeasuredDimension(width, bubble.measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val bubble = getChildAt(1) ?: return
        val avatar = getChildAt(0)
        val height = b - t

        if (showAvatar && avatar != null && avatar.visibility != View.GONE) {
            val avatarTop = height - avatar.measuredHeight
            avatar.layout(0, avatarTop, avatar.measuredWidth, avatarTop + avatar.measuredHeight)
            val bubbleLeft = avatar.measuredWidth + avatarGapPx
            bubble.layout(
                bubbleLeft,
                0,
                bubbleLeft + bubble.measuredWidth,
                bubble.measuredHeight,
            )
        } else {
            avatar?.layout(0, 0, 0, 0)
            val bubbleLeft = avatarOffsetPx
            bubble.layout(
                bubbleLeft,
                0,
                bubbleLeft + bubble.measuredWidth,
                bubble.measuredHeight,
            )
        }
    }
}
