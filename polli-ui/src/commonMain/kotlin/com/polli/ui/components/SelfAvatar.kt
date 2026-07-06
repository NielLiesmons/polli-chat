package com.polli.ui.components

import androidx.compose.foundation.background
import com.polli.ui.components.polliClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.polli.core.sigil.MnsSigil
import com.polli.core.sigil.SigilIdentity
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.ProfileColors

@Composable
fun SelfAvatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    sigilIdentity: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val identity = remember(name, sigilIdentity) {
        SigilIdentity.resolve(sigilIdentity?.trim()?.takeIf { it.isNotEmpty() } ?: name)
    }
    val sigilColor = remember(identity.name) { ProfileColors.stringToColor(identity.name) }
    val clickMod = if (onClick != null) Modifier.polliClickable(onClick = onClick) else Modifier
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(PolliColors.Gray33)
                .then(clickMod),
        contentAlignment = Alignment.Center,
    ) {
        RoundedSigilView(
            value = identity.value,
            modifier = Modifier.fillMaxSize(),
            onColor = sigilColor,
            background = SigilBackground.Transparent,
            contentInsetFraction = 0.12f,
        )
    }
}
