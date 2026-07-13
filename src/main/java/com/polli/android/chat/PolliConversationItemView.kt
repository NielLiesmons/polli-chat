package com.polli.android.chat

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import com.polli.android.icons.PolliIconName
import com.polli.android.platform.PolliAudioPlaybackViewModel
import com.polli.android.theme.PolliDimens
import com.polli.core.chat.MessageGroupLayout
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.OutgoingState

/**
 * DC [ConversationItem] equivalent — pure View bind path with Polli grouped bubble chrome.
 */
class PolliConversationItemView(context: Context) : FrameLayout(context) {
    private val rowPadH = ViewChatUi.dp(context, PolliDimens.ChatRowPaddingH.value)
    private val avatarSize = ViewChatUi.dp(context, PolliDimens.ChatAvatarSize.value)
    private val avatarGap = ViewChatUi.dp(context, PolliDimens.ChatAvatarGap.value)
    private val avatarReserve = avatarSize + avatarGap
    private val bubbleInsetH = ViewChatUi.dp(context, PolliDimens.ChatBubbleInsetH.value)
    private val bubblePadV = ViewChatUi.dp(context, PolliDimens.ChatBubblePaddingV.value)
    private val quotePadH = ViewChatUi.dp(context, PolliDimens.ChatQuoteBubblePadH.value)
    private val richContentPadH = quotePadH

    private val lane = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val avatarView = ImageView(context)
    private val avatarSpacer = Space(context)
    private val bubbleHost = FrameLayout(context)
    private val replyIcon = ImageView(context)
    private val bubbleColumn = MaxWidthLinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
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

    private var touchHelper: ViewBubbleTouchHelper? = null
    private var onTapRoot: ((Float, Float) -> Unit)? = null
    private var maxBubbleWidthPx = 0

    init {
        bodyView.setLineSpacing(0f, 1.15f)

        authorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        authorView.typeface = Typeface.DEFAULT_BOLD
        incomingMetaView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        incomingMetaView.setTextColor(ViewChatUi.whiteAlpha(0.33f))

        outgoingEdited.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        outgoingEdited.setTextColor(ViewChatUi.whiteAlpha(0.33f))
        outgoingTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        outgoingTime.setTextColor(ViewChatUi.whiteAlpha(0.66f))
        outgoingStateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        outgoingStateText.setTextColor(ViewChatUi.whiteAlpha(0.66f))

        check1.setImageResource(PolliIconName.Check.resId)
        check2.setImageResource(PolliIconName.Check.resId)
        val checkSize = ViewChatUi.dp(context, 11f)
        outgoingChecks.addView(check1, FrameLayout.LayoutParams(checkSize, checkSize, Gravity.START or Gravity.CENTER_VERTICAL))
        outgoingChecks.addView(
            check2,
            FrameLayout.LayoutParams(checkSize, checkSize, Gravity.START or Gravity.CENTER_VERTICAL).apply {
                marginStart = ViewChatUi.dp(context, 8f) - ViewChatUi.dp(context, 3f)
            },
        )

        replyIcon.setImageResource(PolliIconName.Reply.resId)
        replyIcon.alpha = 0f

        incomingHeader.addView(authorView, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        incomingHeader.addView(incomingMetaView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        incomingHeader.setPadding(bubbleInsetH, bubblePadV, bubbleInsetH, ViewChatUi.dp(context, 2f))

        reactionsScroll.addView(reactionsRow, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        reactionsScroll.setPadding(
            richContentPadH,
            ViewChatUi.dp(context, PolliDimens.ChatReactionRowTop.value),
            richContentPadH,
            0,
        )

        outgoingMeta.addView(outgoingEdited, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        outgoingMeta.addView(
            outgoingTime,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = ViewChatUi.dp(context, 4f)
            },
        )
        outgoingMeta.addView(
            outgoingStateText,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = ViewChatUi.dp(context, 4f)
            },
        )
        outgoingMeta.addView(
            outgoingChecks,
            LayoutParams(ViewChatUi.dp(context, 16f), LayoutParams.WRAP_CONTENT).apply {
                marginStart = ViewChatUi.dp(context, 4f)
            },
        )
        outgoingMeta.setPadding(
            bubbleInsetH,
            ViewChatUi.dp(context, PolliDimens.ChatBubbleMetaRowPaddingV.value),
            bubbleInsetH,
            0,
        )
        outgoingMeta.translationY = ViewChatUi.dp(context, PolliDimens.ChatBubbleMetaRowMarginTop.value).toFloat()

        quoteBlock.layoutParams =
            LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = quotePadH
                marginEnd = quotePadH
            }

        bubbleColumn.addView(incomingHeader)
        bubbleColumn.addView(quoteBlock)
        bubbleColumn.addView(mediaHost)
        bubbleColumn.addView(bodyView)
        bubbleColumn.addView(reactionsScroll)
        bubbleColumn.addView(outgoingMeta)

        bubbleHost.addView(
            replyIcon,
            LayoutParams(ViewChatUi.dp(context, 20f), ViewChatUi.dp(context, 20f)).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
            },
        )
        bubbleHost.addView(
            bubbleColumn,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END
            },
        )

        avatarView.scaleType = ImageView.ScaleType.CENTER_CROP
        avatarView.clipToOutline = true
        avatarView.outlineProvider =
            object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        lane.addView(avatarView, LayoutParams(avatarSize, avatarSize))
        lane.addView(avatarSpacer, LayoutParams(avatarReserve, 0))
        lane.addView(bubbleHost, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        addView(lane, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
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
        onTap: (rootX: Float, rootY: Float) -> Unit,
        onQuoteClick: (Int) -> Unit,
    ) {
        onTapRoot = onTap
        this.maxBubbleWidthPx = maxBubbleWidthPx
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
        setPadding(startPad, rowTop, endPad, 0)
        alpha = if (highlighted) 1f else 0.98f

        val showAvatar = !message.isOutgoing && layout.isLastInStack
        avatarView.visibility = if (showAvatar) View.VISIBLE else View.GONE
        avatarSpacer.visibility = if (!message.isOutgoing && !showAvatar) View.VISIBLE else View.GONE
        if (showAvatar) {
            avatarView.setImageDrawable(ViewChatUi.avatarDrawable(context, message.authorName, message.authorKey))
        }

        lane.gravity = if (message.isOutgoing) Gravity.END else Gravity.BOTTOM
        (bubbleHost.layoutParams as LinearLayout.LayoutParams).gravity =
            if (message.isOutgoing) Gravity.END else Gravity.START
        (bubbleColumn.layoutParams as FrameLayout.LayoutParams).gravity =
            if (message.isOutgoing) Gravity.END else Gravity.START

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
            authorView.setTextColor(ViewChatUi.authorColor(message.authorKey))
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
        if (bodyHasText) {
            bodyView.visibility = View.VISIBLE
            val emojiOnly = EmojiText.isEmojiOnlyShortText(message.text)
            bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (emojiOnly) 14.5f * EmojiText.EMOJI_MAGNIFY else 14.5f)
            bodyView.setTextColor(if (message.isOutgoing) 0xFFFFFFFF.toInt() else ViewChatUi.whiteAlpha(0.85f))
            ViewChatUi.configureLinkBody(bodyView, message.isOutgoing, context)
            bodyView.text = ViewChatUi.linkifiedBody(message.text, message.isOutgoing, context)
            bodyView.setPadding(bubbleInsetH, bodyTopPad(message), bubbleInsetH, bodyBottomPad(message))
        } else {
            bodyView.visibility = View.GONE
        }

        if (reactions.isEmpty()) {
            reactionsScroll.visibility = View.GONE
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

        if (touchHelper == null) {
            touchHelper = ViewBubbleTouchHelper(bubbleColumn)
        }
        touchHelper?.onSwipeReply = onSwipeReply
        touchHelper?.onDragProgress = { progress -> replyIcon.alpha = progress }
        touchHelper?.onTap = { localX, localY ->
            val loc = IntArray(2)
            bubbleColumn.getLocationInWindow(loc)
            onTapRoot?.invoke(loc[0] + localX, loc[1] + localY)
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

    private fun bodyTopPad(message: ChatMessage): Int = 0

    private fun bodyBottomPad(message: ChatMessage): Int = 0
}
