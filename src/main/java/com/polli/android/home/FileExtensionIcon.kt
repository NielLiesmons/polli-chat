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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens
import com.polli.android.theme.ProfileColors

private val ExtensionLabelStyle =
    TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.15.sp,
        lineHeight = 15.sp,
        textAlign = TextAlign.Center,
        lineHeightStyle =
            LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
    )

/** Rounded-square extension badge — port of zaplab_design LabExtensionIcon. */
@Composable
fun FileExtensionIcon(
    extension: String,
    modifier: Modifier = Modifier,
    size: Dp = PolliDimens.HomeSearchPanelIconSize,
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
                .background(PolliColors.White8),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayText,
                color = PolliColors.White,
                style = ExtensionLabelStyle,
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
                style = ExtensionLabelStyle,
            )
        }
    }
}
