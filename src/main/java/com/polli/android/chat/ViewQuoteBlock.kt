package com.polli.android.chat

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.polli.android.theme.PolliDimens
import com.polli.domain.model.chat.MessageQuote
import com.polli.ui.theme.ProfileColors
import kotlin.math.roundToInt

/** View port of [QuotedMessageBlock] for in-bubble quotes. */
internal class ViewQuoteBlock(context: Context) : LinearLayout(context) {
    private val accentBar = View(context)
    private val textColumn = LinearLayout(context).apply { orientation = VERTICAL }
    private val authorView = TextView(context)
    private val previewView = TextView(context)
    private val cornerPx = ViewChatUi.dp(context, 8f)
    private val accentWidthPx = ViewChatUi.dp(context, PolliDimens.ChatQuoteAccentWidth.value)
    private val marginBottomPx = ViewChatUi.dp(context, PolliDimens.ChatQuoteMarginBottom.value) + ViewChatUi.dp(context, 2f)

    init {
        orientation = HORIZONTAL
        // Clip accent bar to the same 8dp rounded rect on all corners.
        clipToOutline = true
        clipChildren = true
        outlineProvider =
            object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerPx.toFloat())
                }
            }
        background =
            GradientDrawable().apply {
                cornerRadius = cornerPx.toFloat()
                setColor(0x55000000) // PolliColors.Black33
            }
        accentBar.background =
            GradientDrawable().apply {
                setColor(0x55FFFFFF.toInt())
                cornerRadii =
                    floatArrayOf(
                        cornerPx.toFloat(),
                        cornerPx.toFloat(),
                        0f,
                        0f,
                        0f,
                        0f,
                        cornerPx.toFloat(),
                        cornerPx.toFloat(),
                    )
            }
        authorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        authorView.typeface = com.polli.android.theme.PolliTypefaces.bold(context)
        authorView.maxLines = 1
        authorView.ellipsize = TextUtils.TruncateAt.END
        previewView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        previewView.typeface = com.polli.android.theme.PolliTypefaces.regular(context)
        previewView.maxLines = 1
        previewView.ellipsize = TextUtils.TruncateAt.END

        val bodyPadH = ViewChatUi.dp(context, 8f)
        val bodyPadTop = ViewChatUi.dp(context, 5f)
        val bodyPadBottom = ViewChatUi.dp(context, 6f)
        textColumn.setPadding(bodyPadH, bodyPadTop, ViewChatUi.dp(context, 10f), bodyPadBottom)
        textColumn.addView(authorView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        textColumn.addView(
            previewView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = ViewChatUi.dp(context, 1f)
            },
        )
        addView(accentBar, LayoutParams(accentWidthPx, LayoutParams.MATCH_PARENT))
        addView(textColumn, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = marginBottomPx
            }
    }

    fun bind(quote: MessageQuote, isOutgoing: Boolean, onClick: (() -> Unit)?) {
        val preview = quote.text.replace('\n', ' ').trim()
        if (preview.isEmpty() && quote.authorName.isEmpty()) {
            visibility = GONE
            return
        }
        visibility = VISIBLE
        val seed =
            quote.authorColorSeed.ifBlank {
                if (quote.authorId != 0) quote.authorId.toString() else quote.authorName
            }
        val nameColor = ViewChatUi.authorColor(seed.ifBlank { quote.authorName })
        val accentColor =
            if (isOutgoing) {
                nameColor
            } else if (seed.isBlank()) {
                ViewChatUi.whiteAlpha(0.33f)
            } else {
                val c = ProfileColors.stringToColor(seed)
                android.graphics.Color.argb(
                    (0.85f * 255).roundToInt(),
                    (c.red * 255).roundToInt(),
                    (c.green * 255).roundToInt(),
                    (c.blue * 255).roundToInt(),
                )
            }
        (accentBar.background as? GradientDrawable)?.setColor(accentColor)
            ?: accentBar.setBackgroundColor(accentColor)
        if (quote.authorName.isNotEmpty()) {
            authorView.visibility = VISIBLE
            authorView.text = quote.authorName
            authorView.setTextColor(nameColor)
        } else {
            authorView.visibility = GONE
        }
        if (preview.isNotEmpty()) {
            previewView.visibility = VISIBLE
            previewView.text = preview
            previewView.setTextColor(
                if (isOutgoing) ViewChatUi.whiteAlpha(0.85f) else ViewChatUi.whiteAlpha(0.66f),
            )
        } else {
            previewView.visibility = GONE
        }
        isClickable = onClick != null
        setOnClickListener(if (onClick != null) OnClickListener { onClick() } else null)
    }
}
