package com.polli.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.android.theme.LabColors
import dev.chrisbanes.haze.HazeState

@Composable
fun BlurPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = LabColors.Gray66,
    borderColor: Color = LabColors.White16,
    hazeState: HazeState? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    FrostedPanel(
        modifier = modifier,
        cornerRadius = cornerRadius,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        hazeState = hazeState,
        content = content,
    )
}

@Composable
fun FrostedPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    shape: Shape? = null,
    backgroundColor: Color = LabColors.Gray66,
    borderColor: Color = LabColors.White16,
    hazeState: HazeState? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val clipShape = shape ?: RoundedCornerShape(cornerRadius)
    FrostedChromeSurface(
        modifier = modifier,
        shape = clipShape,
        tint = backgroundColor,
        borderColor = borderColor,
        hazeState = hazeState,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            content = content,
        )
    }
}
