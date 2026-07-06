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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** Dimmed scrim behind modals / expanded search — ~37% black (half the old 75% barrier). */
val PolliModalBarrier: Color = Color(0x60000000)

private const val FROST_TINT_ALPHA = 0.72f

/** Shared frosted-glass blur style for Polli chrome surfaces (composer, modals, search bar). */
fun polliHazeStyle(tint: Color = PolliColors.Gray66): HazeStyle = HazeStyle(
    backgroundColor = tint.copy(alpha = FROST_TINT_ALPHA),
    tints = listOf(
        HazeTint(Color.Black.copy(alpha = 0.32f)),
        HazeTint(tint.copy(alpha = 0.52f)),
    ),
    blurRadius = 40.dp,
    noiseFactor = HazeDefaults.noiseFactor,
)

/** Darker frost for the home search pill / expanded panel (solid [PolliColors.Gray], not the lighter Gray33 token). */
fun polliSearchPanelHazeStyle(tint: Color = PolliColors.Gray): HazeStyle = HazeStyle(
    backgroundColor = tint.copy(alpha = 0.94f),
    tints = listOf(
        HazeTint(Color.Black.copy(alpha = 0.58f)),
        HazeTint(tint.copy(alpha = 0.82f)),
    ),
    blurRadius = 36.dp,
    noiseFactor = HazeDefaults.noiseFactor,
)

/**
 * Modal sheet frost — matches zapstore AppModal (gray66 + ~14px backdrop blur).
 * High tint opacity so content does not bleed through when blur is unavailable.
 */
fun polliModalSheetHazeStyle(tint: Color = PolliColors.Gray66): HazeStyle = HazeStyle(
    backgroundColor = tint.copy(alpha = 0.96f),
    tints = listOf(
        HazeTint(Color.Black.copy(alpha = 0.35f)),
        HazeTint(tint.copy(alpha = 0.88f)),
    ),
    blurRadius = 28.dp,
    noiseFactor = HazeDefaults.noiseFactor,
)

/** Heavier frost for bubble overlay panels so feed text does not bleed through. */
fun polliOverlayHazeStyle(tint: Color = PolliColors.Gray66): HazeStyle = HazeStyle(
    backgroundColor = tint.copy(alpha = 0.94f),
    tints = listOf(
        HazeTint(Color.Black.copy(alpha = 0.50f)),
        HazeTint(tint.copy(alpha = 0.72f)),
    ),
    blurRadius = 48.dp,
    noiseFactor = HazeDefaults.noiseFactor,
)


@Composable
fun rememberPolliHazeState(): HazeState = remember { HazeState() }

/**
 * Full-screen tap target behind sheets / expanded search.
 *
 * Flat dim only — blur is applied on frosted chrome surfaces ([FrostedChromeSurface]),
 * never on the scrim. Matches zapstore: barrier is ~65% black, sheet gets backdrop blur.
 */
@Composable
fun PolliScreenScrim(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PolliModalBarrier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onDismiss,
            ),
    )
}

@OptIn(ExperimentalHazeApi::class)
@Composable
fun FrostedChromeSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    tint: Color = PolliColors.Gray66,
    borderColor: Color = PolliColors.ShellBorder,
    hazeState: HazeState? = null,
    hazeStyle: HazeStyle? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val style = hazeStyle ?: polliHazeStyle(tint)
    val frostTint = style.backgroundColor
    val chromeModifier = modifier
        .clip(shape)
        .graphicsLayer { clip = true }
        .border(PolliDimens.ShellBorderWidth, borderColor, shape)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(state = hazeState, style = style) {
                    inputScale = HazeInputScale.Auto
                    backgroundColor = frostTint
                }
            } else {
                Modifier.background(frostTint)
            },
        )
    Box(modifier = chromeModifier, content = content)
}

@Composable
fun FrostedCircleButton(
    onClick: () -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    tint: Color = PolliColors.Gray66,
    borderColor: Color = PolliColors.ShellBorder,
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
