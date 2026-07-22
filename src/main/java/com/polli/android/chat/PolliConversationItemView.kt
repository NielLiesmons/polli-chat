package com.polli.android.chat

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.polli.android.R
import com.polli.android.platform.PolliAudioPlaybackViewModel
import com.polli.android.theme.PolliDimens
import com.polli.core.chat.MessageGroupLayout
import com.polli.domain.model.chat.ChatMessage

/**
 * DC [ConversationItem] equivalent — pure View bind path with Polli grouped bubble chrome.
 * Layout/padding must match [MessageBubble] / [SingleIncomingMessageRow] / [OutgoingMessageRow].
 */
class PolliConversationItemView(context: Context) : FrameLayout(context) {
    private val rowPadH = ViewChatUi.dp(context, PolliDimens.ChatRowPaddingH.value)
    private val avatarSize = ViewChatUi.dp(context, PolliDimens.ChatAvatarSize.value)
    private val avatarGap = ViewChatUi.dp(context, PolliDimens.ChatAvatarGap.value)
    private val bubbleInsetH = ViewChatUi.dp(context, PolliDimens.ChatBubbleInsetH.value)
    private val bubblePadV = ViewChatUi.dp(context, PolliDimens.ChatBubblePaddingV.value)
    private val quotePadH = ViewChatUi.dp(context, PolliDimens.ChatQuoteBubblePadH.value)
    private val richContentPadH = quotePadH

    private val highlightBand = View(context)
    private val incomingLane = IncomingMessageLane(context).apply {
        avatarSizePx = avatarSize
        avatarGapPx = avatarGap
    }
    private val outgoingLane = FrameLayout(context).apply {
        clipChildren = false
        clipToPadding = false
    }
    private val avatarView = ImageView(context)
    private val bubbleColumn = MaxWidthLinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val bubbleHost = BubbleSwipeHost(context, bubbleColumn)
    private val incomingHeader = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val authorView = TextView(context)
    private val incomingMetaView = TextView(context)
    private val quoteBlock = ViewQuoteBlock(context)
    private val mediaHost = FrameLayout(context)
    private val bodyView = TextView(context)
    private val reactionsScroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }
    private val reactionsRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val outgoingMeta = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.END
    }
    private val outgoingEdited = TextView(context)
    private val outgoingTime = TextView(context)
    private val outgoingStateText = TextView(context)
    private val outgoingChecks = FrameLayout(context)
    private val check1 = ImageView(context)
    private val check2 = ImageView(context)

    private var onTapScreen: ((Float, Float) -> Unit)? = null
    private var highlightDrawable = ColorDrawable(ViewChatUi.white8())

    init {
        clipChildren = false
        clipToPadding = false

        highlightBand.background = highlightDrawable
        highlightBand.alpha = 0f
        // Full-bleed band across the row (edge-to-edge of the feed column).
        addView(highlightBand, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        bodyView.setLineSpacing(0f, 1.15f)

        authorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        authorView.typeface = com.polli.android.theme.PolliTypefaces.bold(context)
        bodyView.typeface = com.polli.android.theme.PolliTypefaces.regular(context)
        incomingMetaView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        incomingMetaView.setTextColor(ViewChatUi.whiteAlpha(0.33f))
        incomingMetaView.typeface = com.polli.android.theme.PolliTypefaces.regular(context)

        outgoingEdited.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        outgoingEdited.setTextColor(ViewChatUi.whiteAlpha(0.33f))
        outgoingEdited.typeface = com.polli.android.theme.PolliTypefaces.regular(context)
        outgoingTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        outgoingTime.setTextColor(ViewChatUi.white66())
        outgoingTime.typeface = com.polli.android.theme.PolliTypefaces.regular(context)
        outgoingStateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        outgoingStateText.setTextColor(ViewChatUi.white66())
        outgoingStateText.typeface = com.polli.android.theme.PolliTypefaces.regular(context)

        // DC check.png — tint White66 to match timestamp/Edited (+2px vs prior 11dp).
        check1.setImageResource(R.drawable.check)
        check2.setImageResource(R.drawable.check)
        val checkSize = ViewChatUi.dp(context, 13f)
        val checkColor = ViewChatUi.white66()
        check1.setColorFilter(checkColor)
        check2.setColorFilter(checkColor)
        outgoingChecks.addView(check1, FrameLayout.LayoutParams(checkSize, checkSize, Gravity.START or Gravity.CENTER_VERTICAL))
        outgoingChecks.addView(
            check2,
            FrameLayout.LayoutParams(checkSize, checkSize, Gravity.START or Gravity.CENTER_VERTICAL).apply {
                marginStart = ViewChatUi.dp(context, 5f)
            },
        )

        incomingHeader.addView(
            authorView,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        incomingHeader.addView(
            incomingMetaView,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
        // Shell padding supplies top inset — header only needs horizontal + bottom (Compose parity).
        incomingHeader.setPadding(bubbleInsetH, 0, bubbleInsetH, ViewChatUi.dp(context, 2f))
        incomingHeader.isClickable = true

        reactionsScroll.addView(
            reactionsRow,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )
        reactionsScroll.setPadding(
            richContentPadH,
            ViewChatUi.dp(context, PolliDimens.ChatReactionRowTop.value),
            richContentPadH,
            0,
        )

        outgoingMeta.addView(
            outgoingEdited,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
        outgoingMeta.addView(
            outgoingTime,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = ViewChatUi.dp(context, 4f)
            },
        )
        outgoingMeta.addView(
            outgoingStateText,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = ViewChatUi.dp(context, 4f)
            },
        )
        outgoingMeta.addView(
            outgoingChecks,
            LinearLayout.LayoutParams(ViewChatUi.dp(context, 20f), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = ViewChatUi.dp(context, 4f)
                gravity = Gravity.CENTER_VERTICAL
            },
        )
        // ~3dp less right pad than bubbleInsetH so timestamp/checks sit tighter to the shell edge.
        outgoingMeta.setPadding(
            bubbleInsetH,
            ViewChatUi.dp(context, PolliDimens.ChatBubbleMetaRowPaddingV.value),
            (bubbleInsetH - ViewChatUi.dp(context, 3f)).coerceAtLeast(0),
            0,
        )
        outgoingMeta.translationY = ViewChatUi.dp(context, PolliDimens.ChatBubbleMetaRowMarginTop.value).toFloat()
        outgoingMeta.isClickable = true

        quoteBlock.layoutParams =
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginStart = quotePadH
                marginEnd = quotePadH
            }

        bubbleColumn.addView(incomingHeader)
        bubbleColumn.addView(quoteBlock)
        bubbleColumn.addView(mediaHost)
        bubbleColumn.addView(bodyView)
        bubbleColumn.addView(reactionsScroll)
        bubbleColumn.addView(outgoingMeta)

        avatarView.scaleType = ImageView.ScaleType.CENTER_CROP
        avatarView.clipToOutline = true
        avatarView.outlineProvider =
            object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }

        // Avatar is laid out by IncomingMessageLane and does not contribute to row height.
        incomingLane.addView(avatarView, ViewGroup.LayoutParams(avatarSize, avatarSize))
        // bubbleHost is attached in bind() to incoming or outgoing lane (single parent).
        addView(incomingLane, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(outgoingLane, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        outgoingLane.visibility = View.GONE

        val openOverlayFromView: (View) -> Unit = { view ->
            val loc = IntArray(2)
            view.getLocationOnScreen(loc)
            onTapScreen?.invoke(loc[0] + view.width / 2f, loc[1] + view.height / 2f)
        }
        bodyView.setOnClickListener { openOverlayFromView(it) }
        incomingHeader.setOnClickListener { openOverlayFromView(it) }
        outgoingMeta.setOnClickListener { openOverlayFromView(it) }
        bubbleColumn.setOnClickListener { openOverlayFromView(it) }
    }

    fun setHighlighted(highlighted: Boolean) {
        if (highlighted) {
            highlightBand.animate().alpha(1f).setDuration(120).start()
        } else {
            highlightBand.animate().alpha(0f).setDuration(500).start()
        }
    }

    fun bind(
        message: ChatMessage,
        layout: MessageGroupLayout,
        maxBubbleWidthPx: Int,
        reactions: List<BubbleReaction>,
        pulseEmoji: String?,
        highlighted: Boolean,
        playbackViewModel: PolliAudioPlaybackViewModel?,
        onSwipeReply: () -> Unit,
        onTap: (screenX: Float, screenY: Float) -> Unit,
        onQuoteClick: (Int) -> Unit,
    ) {
        onTapScreen = onTap
        bubbleHost.onTapScreen = onTap
        bubbleColumn.setMaxBubbleWidth(maxBubbleWidthPx)

        val incomingInGroup = !layout.isFirstInStack
        val rowTop =
            if (layout.isFirstInStack) {
                ViewChatUi.dp(context, PolliDimens.ChatRowTop.value)
            } else {
                ViewChatUi.dp(context, PolliDimens.ChatRowTopCollapsed.value)
            }
        val startPad =
            if (message.isOutgoing) {
                rowPadH + ViewChatUi.dp(context, PolliDimens.ChatRowOutgoingExtraStart.value)
            } else {
                rowPadH
            }
        val endPad =
            if (message.isOutgoing) rowPadH else ViewChatUi.dp(context, PolliDimens.ChatRowIncomingRight.value)
        setPadding(0, rowTop, 0, 0)
        (incomingLane.layoutParams as LayoutParams).apply {
            marginStart = startPad
            marginEnd = endPad
        }
        (outgoingLane.layoutParams as LayoutParams).apply {
            marginStart = startPad
            marginEnd = endPad
        }

        setHighlighted(highlighted)

        val showAvatar = !message.isOutgoing && layout.isLastInStack
        attachBubbleLane(outgoing = message.isOutgoing)
        if (!message.isOutgoing) {
            incomingLane.showAvatar = showAvatar
            if (showAvatar) {
                avatarView.visibility = View.VISIBLE
                ViewProfileAvatar.bind(
                    imageView = avatarView,
                    name = message.authorName,
                    seed = message.authorKey,
                    contactId = message.authorId,
                )
            } else {
                avatarView.visibility = View.GONE
            }
        } else {
            avatarView.visibility = View.GONE
        }
        bubbleHost.setBubbleGravity(end = message.isOutgoing)

        bubbleColumn.background =
            if (message.isOutgoing) {
                ViewChatUi.outgoingBubbleBackground(context, layout.isLastInStack)
            } else {
                ViewChatUi.incomingBubbleBackground(context, layout.isLastInStack)
            }
        bubbleColumn.setPadding(0, shellTopPad(message, layout, incomingInGroup), 0, shellBottomPad(message))

        incomingHeader.visibility =
            if (!message.isOutgoing && layout.isFirstInStack) View.VISIBLE else View.GONE
        if (!message.isOutgoing && layout.isFirstInStack) {
            authorView.text = message.authorName
            authorView.setTextColor(ViewChatUi.authorColor(message.authorColorSeed.ifBlank { message.authorKey }))
            incomingMetaView.text = buildMetaLabel(message.isEdited, ViewChatUi.formatTime(message.timestamp))
        }

        message.quote?.let { quote ->
            quoteBlock.bind(quote, message.isOutgoing) { onQuoteClick(quote.msgId) }
        } ?: run {
            quoteBlock.visibility = View.GONE
        }

        val richContentWidthPx = (maxBubbleWidthPx - richContentPadH * 2).coerceAtLeast(0)
        ViewBubbleMedia.bind(
            host = mediaHost,
            message = message,
            contentWidthPx = richContentWidthPx,
            isOutgoing = message.isOutgoing,
            playbackViewModel = playbackViewModel,
            horizontalPadPx = richContentPadH,
        )

        val bodyHasText = message.text.isNotBlank()
        val hasLinks = bodyHasText && MessageLinkify.splitMessageParts(message.text).any { it is MessagePart.Link }
        if (bodyHasText) {
            bodyView.visibility = View.VISIBLE
            val emojiOnly = EmojiText.isEmojiOnlyShortText(message.text)
            bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (emojiOnly) 14.5f * EmojiText.EMOJI_MAGNIFY else 14.5f)
            bodyView.setTextColor(if (message.isOutgoing) 0xFFFFFFFF.toInt() else ViewChatUi.whiteAlpha(0.85f))
            if (hasLinks) {
                ViewChatUi.configureLinkBody(bodyView, message.isOutgoing, context)
                bodyView.text = ViewChatUi.linkifiedBody(message.text, message.isOutgoing, context)
            } else {
                bodyView.movementMethod = null
                bodyView.text = message.text
            }
            bodyView.setPadding(bubbleInsetH, 0, bubbleInsetH, 0)
        } else {
            bodyView.visibility = View.GONE
        }

        if (reactions.isEmpty()) {
            reactionsScroll.visibility = View.GONE
            reactionsRow.removeAllViews()
        } else {
            reactionsScroll.visibility = View.VISIBLE
            ViewReactionPills.populate(reactionsRow, reactions, pulseEmoji)
        }

        outgoingMeta.visibility = if (message.isOutgoing) View.VISIBLE else View.GONE
        if (message.isOutgoing) {
            ViewOutgoingMeta.bind(
                row = outgoingMeta,
                editedView = outgoingEdited,
                timeView = outgoingTime,
                stateText = outgoingStateText,
                checksHost = outgoingChecks,
                check1 = check1,
                check2 = check2,
                message = message,
            )
        }

        bubbleHost.onSwipeReply = onSwipeReply
    }

    /** Light bind for reaction-only updates (pop included). */
    fun bindReactions(reactions: List<BubbleReaction>, pulseEmoji: String?) {
        if (reactions.isEmpty()) {
            reactionsScroll.visibility = View.GONE
            reactionsRow.removeAllViews()
        } else {
            reactionsScroll.visibility = View.VISIBLE
            ViewReactionPills.populate(reactionsRow, reactions, pulseEmoji)
        }
    }

    private fun attachBubbleLane(outgoing: Boolean) {
        val target: ViewGroup = if (outgoing) outgoingLane else incomingLane
        if (bubbleHost.parent === target) {
            incomingLane.visibility = if (outgoing) View.GONE else View.VISIBLE
            outgoingLane.visibility = if (outgoing) View.VISIBLE else View.GONE
            return
        }
        (bubbleHost.parent as? ViewGroup)?.removeView(bubbleHost)
        if (outgoing) {
            outgoingLane.addView(
                bubbleHost,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END),
            )
            incomingLane.visibility = View.GONE
            outgoingLane.visibility = View.VISIBLE
        } else {
            incomingLane.addView(
                bubbleHost,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            incomingLane.visibility = View.VISIBLE
            outgoingLane.visibility = View.GONE
        }
    }

    private fun buildMetaLabel(edited: Boolean, time: String): String =
        buildString {
            if (edited) append("Edited  ")
            append(time)
        }

    private fun shellTopPad(message: ChatMessage, layout: MessageGroupLayout, incomingInGroup: Boolean): Int {
        val hasQuote = message.quote != null
        val hasAttachment = message.hasAttachment
        val bodyHasText = message.text.isNotBlank()
        val textOnlyOutgoing = message.isOutgoing && !hasQuote && !hasAttachment && bodyHasText
        if (message.isOutgoing) {
            val extra =
                if (textOnlyOutgoing) {
                    ViewChatUi.dp(context, PolliDimens.ChatBubbleTextOnlyExtraTop.value)
                } else {
                    0
                }
            return bubblePadV + extra
        }
        val stacked =
            if (!incomingInGroup || layout.isFirstInStack || hasQuote || hasAttachment) {
                0
            } else {
                ViewChatUi.dp(context, PolliDimens.ChatBubbleStackedIncomingTopExtra.value)
            }
        return bubblePadV + stacked
    }

    private fun shellBottomPad(message: ChatMessage): Int =
        if (message.isOutgoing) {
            ViewChatUi.dp(context, PolliDimens.ChatBubbleOutgoingShellBottom.value)
        } else {
            bubblePadV + ViewChatUi.dp(context, PolliDimens.ChatBubbleIncomingBottomExtra.value)
        }
}
