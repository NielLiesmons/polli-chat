package com.polli.ui.home

import androidx.compose.foundation.background
import com.polli.ui.components.polliClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.ui.components.LabIcon
import com.polli.ui.components.LabIconName
import com.polli.ui.theme.LabColors
import com.polli.ui.theme.LabDimens
import com.polli.ui.components.FrostedChromeSurface
import com.polli.ui.components.polliSearchPanelHazeStyle
import dev.chrisbanes.haze.HazeState

/** Frosted glass + shell border — darker than composer chrome. */
@Composable
fun HomeSearchPillSurface(
    cornerRadius: Dp,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    FrostedChromeSurface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        tint = LabColors.Gray,
        borderColor = LabColors.ShellBorder,
        hazeState = hazeState,
        hazeStyle = polliSearchPanelHazeStyle(),
        content = content,
    )
}

/** In-pill + button — fixed size; icon rotates (45° = ×) and fades as the panel expands. */
@Composable
fun HomeSearchActionButton(
    expandProgress: Float,
    searchPanelOpen: Boolean,
    onPlusClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = searchPanelOpen || expandProgress > 0.5f
    val fade = expandProgress.coerceIn(0f, 1f)
    val bgAlpha = 0.08f + (0.05f - 0.08f) * fade
    val iconAlpha = 0.33f + (0.22f - 0.33f) * fade
    val rotation = expandProgress * 45f
    Box(
        modifier = modifier
            .size(LabDimens.HomePillActionSize)
            .clip(CircleShape)
            .background(LabColors.White.copy(alpha = bgAlpha))
            .polliClickable {
                if (expanded) onCloseClick() else onPlusClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        LabIcon(
            LabIconName.Plus,
            LabDimens.HomeSearchPlusGlyphSize,
            LabColors.White.copy(alpha = iconAlpha),
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
        )
    }
}
