package com.polli.android.chat

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * DC-style swipe-right-to-reply + tap for overlay — pure View, no Compose.
 */
internal class ViewBubbleTouchHelper(
    private val target: View,
) {
    var onSwipeReply: () -> Unit = {}
    var onTap: (localX: Float, localY: Float) -> Unit = { _, _ -> }
    var onDragProgress: (progress: Float) -> Unit = {}
    private val slop = ViewConfiguration.get(target.context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private var downX = 0f
    private var downY = 0f
    private var downUptime = 0L
    private var dragging = false
    private var triggered = false

    init {
        target.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    downUptime = event.eventTime
                    dragging = false
                    triggered = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - downX
                    if (!dragging && dx > slop && abs(event.y - downY) < slop * 2) {
                        dragging = true
                    }
                    if (dragging && dx > 0) {
                        val clamped = dx.coerceIn(0f, MAX_DRAG)
                        target.translationX = clamped
                        onDragProgress((clamped / TRIGGER_AT).coerceIn(0f, 1f))
                        if (clamped >= TRIGGER_AT && !triggered) {
                            triggered = true
                            onSwipeReply()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    val duration = event.eventTime - downUptime
                    if (!dragging && !triggered &&
                        duration < longPressTimeout &&
                        abs(dx) < slop &&
                        abs(dy) < slop
                    ) {
                        onTap(event.x, event.y)
                    }
                    target.animate().translationX(0f).setDuration(180).start()
                    onDragProgress(0f)
                    dragging = false
                    triggered = false
                    true
                }
                else -> false
            }
        }
    }

    private companion object {
        const val MAX_DRAG = 92f
        const val TRIGGER_AT = 36f
    }
}
