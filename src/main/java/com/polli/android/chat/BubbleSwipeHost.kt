package com.polli.android.chat

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.polli.android.icons.PolliIconName
import kotlin.math.abs

/**
 * Swipe-right-to-reply + tap — translates [bubble] only (not the reply affordance child).
 * Matches [BubbleSwiper]: Gray33 circle, White33 glyph, trigger at longer drag with pop.
 */
internal class BubbleSwipeHost(
    context: Context,
    private val bubble: View,
) : FrameLayout(context) {
    var onSwipeReply: () -> Unit = {}
    var onTapScreen: (screenX: Float, screenY: Float) -> Unit = { _, _ -> }

    private val replyCircle =
        FrameLayout(context).apply {
            background =
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0x54333333) // PolliColors.Gray33
                }
            alpha = 0f
            visibility = INVISIBLE
            scaleX = 0.5f
            scaleY = 0.5f
        }
    private val replyIcon =
        ImageView(context).apply {
            setImageResource(PolliIconName.Reply.resId)
            setColorFilter(ViewChatUi.whiteAlpha(0.33f), android.graphics.PorterDuff.Mode.SRC_IN)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageAlpha = (0.33f * 255).toInt()
        }

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var downUptime = 0L
    private var dragging = false
    private var triggered = false
    private var popping = false

    init {
        clipChildren = false
        clipToPadding = false
        val circleSize = ViewChatUi.dp(context, ICON_SIZE)
        val glyph = ViewChatUi.dp(context, ICON_GLYPH)
        replyCircle.addView(
            replyIcon,
            LayoutParams(glyph, glyph, Gravity.CENTER),
        )
        addView(
            replyCircle,
            LayoutParams(circleSize, circleSize).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                marginStart = ViewChatUi.dp(context, 4f)
            },
        )
        addView(
            bubble,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END
            },
        )
    }

    fun setBubbleGravity(end: Boolean) {
        (bubble.layoutParams as LayoutParams).gravity =
            if (end) Gravity.END else Gravity.START
        (replyCircle.layoutParams as LayoutParams).marginStart =
            ViewChatUi.dp(context, if (end) 4f else 8f)
        requestLayout()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                downUptime = ev.eventTime
                dragging = false
                triggered = false
                popping = false
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                if (!dragging && dx > slop && abs(ev.y - downY) < slop * 2) {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!dragging) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (popping) return true
                val dx = (ev.x - downX).coerceAtLeast(0f)
                val clamped = dx.coerceIn(0f, MAX_DRAG)
                bubble.translationX = clamped
                // Parallax circle slightly left as bubble moves out.
                val parallax = (-clamped * ICON_PARALLAX).coerceIn(-ICON_MAX_OUTWARD, 0f)
                replyCircle.translationX = parallax
                val progress = (clamped / TRIGGER_AT).coerceIn(0f, 1f)
                replyCircle.visibility = if (progress > 0.01f) VISIBLE else INVISIBLE
                replyCircle.alpha = progress
                val scale = 0.5f + 0.5f * progress
                replyCircle.scaleX = scale
                replyCircle.scaleY = scale
                if (clamped >= TRIGGER_AT && !triggered) {
                    fireReplyPop()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!popping) {
                    settleVisuals()
                    dragging = false
                    triggered = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
                return true
            }
        }
        return true
    }

    private fun fireReplyPop() {
        if (triggered) return
        triggered = true
        popping = true
        replyCircle.visibility = VISIBLE
        replyCircle.alpha = 1f
        onSwipeReply()
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(replyCircle, View.SCALE_X, replyCircle.scaleX, POP_SCALE, 1f),
                ObjectAnimator.ofFloat(replyCircle, View.SCALE_Y, replyCircle.scaleY, POP_SCALE, 1f),
            )
            duration = POP_SETTLE_MS
            interpolator = OvershootInterpolator(1.4f)
            start()
        }
        bubble.animate()
            .translationX(0f)
            .setDuration(POP_SETTLE_MS)
            .withEndAction {
                replyCircle.animate().alpha(0f).setDuration(140).withEndAction {
                    replyCircle.visibility = INVISIBLE
                    replyCircle.scaleX = 0.5f
                    replyCircle.scaleY = 0.5f
                    replyCircle.translationX = 0f
                    popping = false
                    dragging = false
                    triggered = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                }.start()
            }
            .start()
    }

    private fun settleVisuals() {
        bubble.animate().translationX(0f).setDuration(180).start()
        replyCircle.animate().alpha(0f).translationX(0f).setDuration(140).withEndAction {
            replyCircle.visibility = INVISIBLE
            replyCircle.scaleX = 0.5f
            replyCircle.scaleY = 0.5f
        }.start()
    }

    private companion object {
        const val MAX_DRAG = 100f
        /** Longer pull before reply fires — aligned with pop peak. */
        const val TRIGGER_AT = 80f
        const val ICON_PARALLAX = 0.28f
        const val ICON_MAX_OUTWARD = 21f
        const val ICON_SIZE = 28f
        const val ICON_GLYPH = 13f
        const val POP_SCALE = 1.22f
        const val POP_SETTLE_MS = 180L
    }
}
