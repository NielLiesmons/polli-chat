package com.polli.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.theme.ProfileColors

/** Rounded-square extension badge — port of zaplab_design LabExtensionIcon. */
@Composable
fun FileExtensionIcon(
    extension: String,
    modifier: Modifier = Modifier,
    size: Dp = LabDimens.HomeSearchPanelIconSize,
) {
    val cleanExtension = extension.trim().uppercase().removePrefix(".")
    val displayText = cleanExtension.take(4)
    val extensionColor = ProfileColors.stringToColor(cleanExtension)
    val cornerRadius = (size.value * 12f / 38f).dp
    val shape = RoundedCornerShape(cornerRadius)

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(LabColors.White8),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayText,
                color = LabColors.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(extensionColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayText,
                color = extensionColor.copy(alpha = 0.90f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
