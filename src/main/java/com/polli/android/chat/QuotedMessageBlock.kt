package com.polli.android.chat

import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.MessageQuote
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
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens
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
        QuotedMessageStyle.Composer -> PolliColors.White8 to PolliColors.White66
        QuotedMessageStyle.InIncomingBubble -> PolliColors.Black33 to PolliColors.White66
        QuotedMessageStyle.InOutgoingBubble -> PolliColors.Black33 to PolliColors.White85
    }
    val bodyPad = when (style) {
        QuotedMessageStyle.Composer ->
            PaddingValues(top = 4.dp, end = 28.dp, bottom = 5.dp, start = 8.dp)
        else ->
            PaddingValues(top = 5.dp, end = 10.dp, bottom = 6.dp, start = 8.dp)
    }
    val marginBottom = when (style) {
        QuotedMessageStyle.Composer -> 0.dp
        else -> PolliDimens.ChatQuoteMarginBottom
    }

    val shell = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
        .padding(bottom = marginBottom)
        .clip(RoundedCornerShape(8.dp))
        .background(bg)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    if (style == QuotedMessageStyle.Composer && onClear != null) {
        Box(modifier = modifier.then(shell)) {
            QuoteBodyRow(
                accent = accent,
                nameColor = nameColor,
                quote = quote,
                preview = preview,
                previewColor = previewColor,
                bodyPad = bodyPad,
            )
            Text(
                text = "×",
                color = PolliColors.White33,
                fontSize = 22.sp,
                lineHeight = 22.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .clickable(onClick = onClear),
            )
        }
        return
    }

    Row(
        modifier = modifier.then(shell),
        verticalAlignment = Alignment.Top,
    ) {
        QuoteAccentBar(accent = accent)
        Row(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 0.dp)
                .padding(bodyPad),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            QuoteTextColumn(
                quote = quote,
                preview = preview,
                nameColor = nameColor,
                previewColor = previewColor,
            )
            if (onClear != null) {
                Text(
                    text = "×",
                    color = PolliColors.White33,
                    fontSize = 20.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable(onClick = onClear),
                )
            }
        }
    }
}

@Composable
private fun QuoteBodyRow(
    accent: Color,
    nameColor: Color,
    quote: MessageQuote,
    preview: String,
    previewColor: Color,
    bodyPad: PaddingValues,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
    ) {
        QuoteAccentBar(accent = accent)
        QuoteTextColumn(
            quote = quote,
            preview = preview,
            nameColor = nameColor,
            previewColor = previewColor,
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 0.dp)
                .padding(bodyPad),
        )
    }
}

@Composable
private fun QuoteAccentBar(accent: Color) {
    Box(
        modifier = Modifier
            .width(PolliDimens.ChatQuoteAccentWidth)
            .fillMaxHeight()
            .background(accent),
    )
}

@Composable
private fun QuoteTextColumn(
    quote: MessageQuote,
    preview: String,
    nameColor: Color,
    previewColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
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
}

private fun previewText(body: String): String =
    body.replace('\n', ' ').trim()

private fun quoteColors(quote: MessageQuote, style: QuotedMessageStyle): Pair<Color, Color> {
    val seed = quoteColorSeed(quote)
    val name = ProfileColors.authorNameColor(seed).copy(alpha = 0.85f)
    if (style == QuotedMessageStyle.InOutgoingBubble) {
        return name to name
    }
    if (seed.isBlank()) return PolliColors.White33 to PolliColors.White33
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
