package com.polli.android.chat

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** View port of [ReactionPillsRow]. */
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
                    if (pulseEmoji == reaction.emoji) {
                        scaleX = 1.15f
                        scaleY = 1.15f
                    }
                }
            val emoji =
                TextView(context).apply {
                    text = reaction.emoji
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setTextColor(0xFFFFFFFF.toInt())
                }
            pill.addView(emoji, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
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
                                    override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                                        outline.setOval(0, 0, view.width, view.height)
                                    }
                                }
                            setImageDrawable(
                                ViewChatUi.avatarDrawable(context, reactor.name, reactor.contactId.toString()),
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
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        marginStart = pad
                    },
                )
            }
            row.addView(
                pill,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = pad
                },
            )
        }
    }
}
