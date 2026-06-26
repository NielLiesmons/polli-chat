package com.polli.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.ui.FrostedChromeSurface
import com.polli.android.ui.polliSearchPanelHazeStyle
import dev.chrisbanes.haze.HazeState

/** Dark frosted glass + shell border — matches chat composer chrome. */
@Composable
fun HomeSearchPillSurface(
    cornerRadius: Dp,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    if (hazeState != null) {
        FrostedChromeSurface(
            modifier = modifier,
            shape = shape,
            tint = LabColors.Gray33,
            borderColor = LabColors.ShellBorder,
            hazeState = hazeState,
            hazeStyle = polliSearchPanelHazeStyle(),
            content = content,
        )
    } else {
        FrostedChromeSurface(
            modifier = modifier,
            shape = shape,
            tint = LabColors.Gray33.copy(alpha = 0.33f),
            borderColor = LabColors.ShellBorder,
            hazeState = null,
            content = content,
        )
    }
}

/** In-pill + button — matches chat composer; rotates with panel expand (45° = ×). */
@Composable
fun HomeSearchActionButton(
    expandProgress: Float,
    searchPanelOpen: Boolean,
    onPlusClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = searchPanelOpen || expandProgress > 0.5f
    val rotation = expandProgress * 45f
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(LabColors.White8)
            .clickable {
                if (expanded) onCloseClick() else onPlusClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        LabIcon(
            LabIconName.Plus,
            LabDimens.HomeSearchGlyphSize,
            LabColors.White33,
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
        )
    }
}
