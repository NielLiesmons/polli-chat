package com.polli.android.chat

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.widget.TextView
import com.polli.android.contacts.avatars.GeneratedContactPhoto
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens
import com.polli.ui.theme.AccentThemes
import com.polli.ui.theme.ProfileColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

internal object ViewChatUi {
    private val timeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    fun dp(context: Context, value: Float): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    fun formatTime(ts: Long): String {
        if (ts <= 0) return ""
        val millis = if (ts < 1_000_000_000_000L) ts * 1000 else ts
        return timeFormat.get().format(Date(millis))
    }

    fun authorColor(seed: String): Int {
        val c = ProfileColors.authorNameColor(seed)
        return Color.argb(
            (c.alpha * 255 * 0.85f).roundToInt().coerceIn(0, 255),
            (c.red * 255).roundToInt(),
            (c.green * 255).roundToInt(),
            (c.blue * 255).roundToInt(),
        )
    }

    fun avatarColor(seed: String): Int {
        val c = ProfileColors.stringToColor(seed.ifBlank { "#" })
        return Color.rgb(
            (c.red * 255).roundToInt(),
            (c.green * 255).roundToInt(),
            (c.blue * 255).roundToInt(),
        )
    }

    fun outgoingBubbleBackground(context: Context, isLastInStack: Boolean): GradientDrawable {
        val preset = AppPrefs(context).accentPreset
        val palette = AccentThemes.palette(preset)
        val start = palette.gradientStart
        val end = palette.gradientEnd
        return GradientDrawable(
            GradientDrawable.Orientation.BL_TR,
            intArrayOf(
                Color.rgb((start.red * 255).roundToInt(), (start.green * 255).roundToInt(), (start.blue * 255).roundToInt()),
                Color.rgb((end.red * 255).roundToInt(), (end.green * 255).roundToInt(), (end.blue * 255).roundToInt()),
            ),
        ).apply {
            setCornerRadii(bubbleRadii(outgoing = true, isLastInStack, context))
        }
    }

    fun incomingBubbleBackground(context: Context, isLastInStack: Boolean): GradientDrawable =
        GradientDrawable().apply {
            setColor(0xFF666666.toInt())
            setCornerRadii(bubbleRadii(outgoing = false, isLastInStack, context))
        }

    private fun bubbleRadii(outgoing: Boolean, isLastInStack: Boolean, context: Context): FloatArray {
        val full = dp(context, PolliDimens.ChatBubbleRadius.value).toFloat()
        val tail = dp(context, PolliDimens.ChatBubbleTailRadius.value).toFloat()
        return if (outgoing) {
            if (isLastInStack) floatArrayOf(full, full, full, full, tail, tail, full, full)
            else floatArrayOf(full, full, full, full, full, full, full, full)
        } else {
            if (isLastInStack) floatArrayOf(full, full, full, full, full, full, tail, tail)
            else floatArrayOf(full, full, full, full, full, full, full, full)
        }
    }

    fun avatarDrawable(context: Context, name: String, seed: String) =
        GeneratedContactPhoto(name.ifBlank { "?" }).asDrawable(context, avatarColor(seed), true)

    fun linkifiedBody(text: String, isOutgoing: Boolean, context: Context): CharSequence {
        val parts = MessageLinkify.splitMessageParts(text)
        if (parts.size == 1 && parts[0] is MessagePart.Text) return text
        val linkColor = if (isOutgoing) 0xEBFFFFFF.toInt() else incomingLinkColor(context)
        val out = SpannableStringBuilder()
        for (part in parts) {
            when (part) {
                is MessagePart.Text -> out.append(part.value)
                is MessagePart.Link -> {
                    val start = out.length
                    out.append(part.label)
                    out.setSpan(URLSpan(part.href), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
        return out
    }

    fun configureLinkBody(textView: TextView, isOutgoing: Boolean, context: Context) {
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = Color.argb(40, 255, 255, 255)
        textView.setLinkTextColor(if (isOutgoing) 0xEBFFFFFF.toInt() else incomingLinkColor(context))
    }

    private fun incomingLinkColor(context: Context): Int {
        val preset = AppPrefs(context).accentPreset
        val palette = AccentThemes.palette(preset)
        val c = palette.gradientStart
        return Color.rgb(
            (c.red * 255).roundToInt(),
            (c.green * 255).roundToInt(),
            (c.blue * 255).roundToInt(),
        )
    }

    fun whiteAlpha(alpha: Float): Int =
        Color.argb((alpha * 255).roundToInt().coerceIn(0, 255), 255, 255, 255)

    fun gray66(): Int = 0xFF666666.toInt()
}
