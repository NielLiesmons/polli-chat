package com.polli.android.icons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import org.thoughtcrime.securesms.R

enum class LabIconName(@DrawableRes val resId: Int) {
    Bell(R.drawable.ic_polli_bell),
    Search(R.drawable.ic_polli_search),
    Plus(R.drawable.ic_polli_plus),
    Cross(R.drawable.ic_polli_cross),
    Delete(R.drawable.ic_polli_delete),
    ChevronLeft(R.drawable.ic_polli_chevron_left),
    ChevronDown(R.drawable.ic_polli_chevron_down),
    ArrowUp(R.drawable.ic_polli_arrow_up),
    ArrowDown(R.drawable.ic_polli_arrow_down),
    Reply(R.drawable.ic_polli_reply),
    Options(R.drawable.ic_polli_options),
    Send(R.drawable.ic_polli_send),
    Check(R.drawable.ic_polli_check),
    EmojiFill(R.drawable.ic_polli_emoji_fill),
    Voice(R.drawable.ic_polli_voice),
    Lock(R.drawable.ic_lock),
    Camera(R.drawable.ic_polli_camera),
    Play(R.drawable.ic_polli_play),
    Pause(R.drawable.ic_polli_pause),
}

@Composable
fun LabIcon(
    icon: LabIconName,
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(icon.resId),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier.size(size),
    )
}
