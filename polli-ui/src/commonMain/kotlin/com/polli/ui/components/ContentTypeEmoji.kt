package com.polli.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.polli.ui.resources.Res
import com.polli.ui.resources.article
import com.polli.ui.resources.chat
import com.polli.ui.resources.event
import com.polli.ui.resources.note
import com.polli.ui.resources.poll
import com.polli.ui.resources.task
import com.polli.ui.theme.PolliDimens
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Zapstore content-type emoji (PNG) — used in the home search “Create” row. */
@Composable
fun ContentTypeEmoji(
    contentType: String,
    modifier: Modifier = Modifier,
    size: Dp = PolliDimens.HomeSearchPanelIconSize,
) {
    Image(
        painter = painterResource(contentTypeDrawable(contentType)),
        contentDescription = contentType,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

private fun contentTypeDrawable(contentType: String): DrawableResource =
    when (contentType.lowercase()) {
        "task" -> Res.drawable.task
        "note" -> Res.drawable.note
        "event" -> Res.drawable.event
        "poll" -> Res.drawable.poll
        "article" -> Res.drawable.article
        "chat" -> Res.drawable.chat
        else -> Res.drawable.chat
    }
