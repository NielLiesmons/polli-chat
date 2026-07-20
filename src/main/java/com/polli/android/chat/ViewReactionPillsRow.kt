package com.polli.android.chat

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** View port of [ReactionPillsRow] — includes pop when [pulseEmoji] matches. */
internal object ViewReactionPills {
    fun populate(
        row: LinearLayout,
        reactions: List<BubbleReaction>,
        pulseEmoji: String?,
    ) {
        row.removeAllViews()
        if (reactions.isEmpty()) return
        val context = row.context
        val pad = ViewChatUi.dp(context, 4f)
        val avatarSize = ViewChatUi.dp(context, 20f)
        val avatarStep = ViewChatUi.dp(context, 14f)
        for (reaction in reactions) {
            val pill =
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background =
                        GradientDrawable().apply {
                            cornerRadius = ViewChatUi.dp(context, 999f).toFloat()
                            setColor(0x14FFFFFF)
                        }
                    val endPad = if (reaction.count > 3) ViewChatUi.dp(context, 6f) else pad
                    setPadding(ViewChatUi.dp(context, 5f), ViewChatUi.dp(context, 3f), endPad, ViewChatUi.dp(context, 3f))
                }
            val emoji =
                TextView(context).apply {
                    text = reaction.emoji
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setTextColor(0xFFFFFFFF.toInt())
                }
            pill.addView(
                emoji,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
            if (reaction.count <= 3 && reaction.reactors.isNotEmpty()) {
                val stack = FrameLayout(context)
                val stackWidth = avatarSize + avatarStep * (reaction.reactors.size - 1).coerceAtLeast(0)
                reaction.reactors.forEachIndexed { index, reactor ->
                    val avatar =
                        ImageView(context).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            clipToOutline = true
                            outlineProvider =
                                object : android.view.ViewOutlineProvider() {
                                    override fun getOutline(view: View, outline: android.graphics.Outline) {
                                        outline.setOval(0, 0, view.width, view.height)
                                    }
                                }
                            ViewProfileAvatar.bind(
                                imageView = this,
                                name = reactor.name,
                                seed = reactor.contactId.toString(),
                                contactId = reactor.contactId,
                            )
                        }
                    stack.addView(
                        avatar,
                        FrameLayout.LayoutParams(avatarSize, avatarSize).apply {
                            marginStart = avatarStep * index
                        },
                    )
                }
                pill.addView(
                    stack,
                    LinearLayout.LayoutParams(stackWidth, avatarSize).apply { marginStart = pad },
                )
            } else if (reaction.count > 3) {
                val count =
                    TextView(context).apply {
                        text = reaction.count.toString()
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                        setTextColor(ViewChatUi.whiteAlpha(0.66f))
                    }
                pill.addView(
                    count,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        marginStart = pad
                    },
                )
            }
            row.addView(
                pill,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    marginEnd = pad
                },
            )
            if (pulseEmoji == reaction.emoji) {
                playPop(pill)
            }
        }
    }

    private fun playPop(target: View) {
        target.scaleX = 0.55f
        target.scaleY = 0.55f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(target, View.SCALE_X, 0.55f, 1.22f, 1f),
                ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.55f, 1.22f, 1f),
            )
            duration = 420
            interpolator = OvershootInterpolator(1.6f)
            start()
        }
    }
}
