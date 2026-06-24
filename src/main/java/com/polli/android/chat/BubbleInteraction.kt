package com.polli.android.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Absorbs taps on bubble sub-regions so the bubble actions overlay does not open.
 * Mirrors polli [bubble_actions_tap_allowed] (quotes, media, reactions, etc.).
 */
fun Modifier.consumeBubbleOverlayTap(): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {},
    )
}
