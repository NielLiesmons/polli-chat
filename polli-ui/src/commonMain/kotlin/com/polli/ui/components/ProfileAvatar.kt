package com.polli.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.ui.theme.ProfileColor

/** Initials avatar for onboarding and list rows without engine photo loading. */
@Composable
fun ProfileAvatar(
    name: String,
    seed: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(ProfileColor.background(seed)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = ProfileColor.text(seed),
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
