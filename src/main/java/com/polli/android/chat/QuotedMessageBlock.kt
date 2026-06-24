package com.polli.android.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.theme.ProfileColors

enum class QuotedMessageStyle { Composer, InIncomingBubble, InOutgoingBubble }

private val QuoteTextStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 19.5.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

@Composable
fun QuotedMessageBlock(
    quote: MessageQuote,
    style: QuotedMessageStyle,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
) {
    val preview = previewText(quote.text)
    if (preview.isEmpty() && quote.authorName.isEmpty()) return

    val (accent, nameColor) = quoteColors(quote, style)
    val (bg, previewColor) = when (style) {
        QuotedMessageStyle.Composer -> LabColors.White8 to LabColors.White66
        QuotedMessageStyle.InIncomingBubble -> LabColors.Black33 to LabColors.White66
        QuotedMessageStyle.InOutgoingBubble -> LabColors.Black33 to LabColors.White85
    }
    val bodyPad = when (style) {
        QuotedMessageStyle.Composer ->
            PaddingValues(top = 4.dp, end = 10.dp, bottom = 5.dp, start = 8.dp)
        else ->
            PaddingValues(top = 5.dp, end = 10.dp, bottom = 6.dp, start = 8.dp)
    }
    val marginBottom = when (style) {
        QuotedMessageStyle.Composer -> 0.dp
        else -> LabDimens.ChatQuoteMarginBottom
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(bottom = marginBottom)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(LabDimens.ChatQuoteAccentWidth)
                .fillMaxHeight()
                .background(accent),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 0.dp)
                .padding(bodyPad),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp),
            ) {
                if (quote.authorName.isNotEmpty()) {
                    Text(
                        text = quote.authorName,
                        color = nameColor,
                        style = QuoteTextStyle.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 1.dp),
                    )
                }
                if (preview.isNotEmpty()) {
                    Text(
                        text = preview,
                        color = previewColor,
                        style = QuoteTextStyle.copy(fontWeight = FontWeight.Normal),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
            }
            if (onClear != null) {
                Text(
                    text = "×",
                    color = LabColors.White33,
                    fontSize = if (style == QuotedMessageStyle.Composer) 22.sp else 20.sp,
                    lineHeight = if (style == QuotedMessageStyle.Composer) 22.sp else 20.sp,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .offset(
                            y = if (style == QuotedMessageStyle.Composer) (-3).dp else 0.dp,
                            x = if (style == QuotedMessageStyle.Composer) (-2).dp else 0.dp,
                        )
                        .clickable(onClick = onClear),
                )
            }
        }
    }
}

private fun previewText(body: String): String =
    body.replace('\n', ' ').trim()

private fun quoteColors(quote: MessageQuote, style: QuotedMessageStyle): Pair<Color, Color> {
    val seed = quoteColorSeed(quote)
    val name = ProfileColors.authorNameColor(seed).copy(alpha = 0.85f)
    if (style == QuotedMessageStyle.InOutgoingBubble) {
        return name to name
    }
    if (seed.isBlank()) return LabColors.White33 to LabColors.White33
    val accent = ProfileColors.stringToColor(seed).copy(alpha = 0.85f)
    return accent to name
}

private fun quoteColorSeed(quote: MessageQuote): String {
    if (quote.authorColorSeed.isNotBlank()) return quote.authorColorSeed
    return if (quote.authorId != 0) quote.authorId.toString() else quote.authorName
}

fun ChatMessage.toReplyQuote(): MessageQuote = MessageQuote(
    msgId = id,
    text = text.ifBlank { fileName ?: "[attachment]" },
    authorId = authorId,
    authorName = authorName,
    authorColorSeed = authorColorSeed,
)
