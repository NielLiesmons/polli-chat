package com.polli.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** zapstore / webapp `.bg-overlay` — ~75% black behind modal sheets. */
val PolliModalBarrier: Color = Color(0xBF000000)

private const val FROST_TINT_ALPHA = 0.72f

/** Shared frosted-glass blur style for Polli chrome surfaces. */
fun polliHazeStyle(tint: Color = LabColors.Gray66): HazeStyle = HazeStyle(
    backgroundColor = tint.copy(alpha = FROST_TINT_ALPHA),
    tints = listOf(
        HazeTint(Color.Black.copy(alpha = 0.32f)),
        HazeTint(tint.copy(alpha = 0.52f)),
    ),
    blurRadius = 40.dp,
    noiseFactor = HazeDefaults.noiseFactor,
)

/** Heavier frost for bubble overlay panels so feed text does not bleed through. */
fun polliOverlayHazeStyle(tint: Color = LabColors.Gray66): HazeStyle = HazeStyle(
    backgroundColor = tint.copy(alpha = 0.94f),
    tints = listOf(
        HazeTint(Color.Black.copy(alpha = 0.50f)),
        HazeTint(tint.copy(alpha = 0.72f)),
    ),
    blurRadius = 48.dp,
    noiseFactor = HazeDefaults.noiseFactor,
)

/** Home search panel — darker and more transparent than chat composer (≈ gray33 @ 33%). */
fun polliSearchPanelHazeStyle(): HazeStyle = HazeStyle(
    backgroundColor = LabColors.Gray33.copy(alpha = 0.33f),
    tints = listOf(
        HazeTint(Color.Black.copy(alpha = 0.62f)),
        HazeTint(LabColors.Gray33.copy(alpha = 0.42f)),
    ),
    blurRadius = 44.dp,
    noiseFactor = HazeDefaults.noiseFactor,
)

@Composable
fun rememberPolliHazeState(): HazeState = remember { HazeState() }

@OptIn(ExperimentalHazeApi::class)
@Composable
fun FrostedChromeSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    tint: Color = LabColors.Gray66,
    borderColor: Color = LabColors.ShellBorder,
    hazeState: HazeState? = null,
    hazeStyle: HazeStyle? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val style = hazeStyle ?: polliHazeStyle(tint)
    val frostTint = style.backgroundColor
    val chromeModifier = modifier
        .clip(shape)
        .border(LabDimens.ShellBorderWidth, borderColor, shape)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(state = hazeState, style = style) {
                    inputScale = HazeInputScale.Auto
                    backgroundColor = frostTint
                }
            } else {
                Modifier.background(tint)
            },
        )
    Box(modifier = chromeModifier, content = content)
}

@Composable
fun FrostedCircleButton(
    onClick: () -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    tint: Color = LabColors.Gray66,
    borderColor: Color = LabColors.ShellBorder,
    content: @Composable BoxScope.() -> Unit,
) {
    FrostedChromeSurface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.CircleShape,
        tint = tint,
        borderColor = borderColor,
        hazeState = hazeState,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
