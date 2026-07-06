package com.polli.ui.components

import androidx.compose.foundation.background
import com.polli.ui.components.polliClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.ui.theme.LabColors
import com.polli.ui.theme.LabDimens

@Composable
fun RoundBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(LabDimens.DetailBackButtonSize)
                .clip(CircleShape)
                .background(LabColors.Gray66)
                .polliClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("‹", color = LabColors.White33, fontSize = 22.sp)
    }
}
