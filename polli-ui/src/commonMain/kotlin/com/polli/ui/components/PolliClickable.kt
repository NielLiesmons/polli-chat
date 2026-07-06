package com.polli.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Tap target without ripple or desktop hover wash.
 * Polli never changes background color on hover — see design system.
 */
@Composable
fun Modifier.polliClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier =
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
