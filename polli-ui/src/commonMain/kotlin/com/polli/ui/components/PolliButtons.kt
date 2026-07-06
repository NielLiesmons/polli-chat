package com.polli.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.ui.theme.PolliColors
import com.polli.ui.theme.accent

@Composable
fun PolliPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: Dp = 48.dp,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(if (enabled) accent().solid else accent().solid.copy(alpha = 0.4f))
                .polliClickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        PolliButtonLabel(label, color = PolliColors.White)
    }
}

@Composable
fun PolliGhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = PolliColors.White33,
) {
    Box(
        modifier =
            modifier
                .polliClickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        PolliButtonLabel(label, color = color, fontSize = 14.sp)
    }
}

@Composable
internal fun PolliButtonLabel(
    label: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
) {
    Text(
        text = label,
        color = color,
        style =
            TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                lineHeight = fontSize,
            ),
    )
}
