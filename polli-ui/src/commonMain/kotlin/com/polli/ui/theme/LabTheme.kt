package com.polli.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.polli.domain.prefs.UiPreferences

val LocalUiPreferences = staticCompositionLocalOf<UiPreferences> {
    error("UiPreferences not provided")
}

@Composable
fun LabTheme(
    prefs: UiPreferences,
    uiScaleRevision: Int = 0,
    content: @Composable () -> Unit,
) {
    val accentPalette = rememberAccentPalette(prefs, prefsRevision = uiScaleRevision)
    val scaledDensity = rememberScaledDensity(prefs, uiScaleRevision)
    val colorScheme = darkColorScheme(
        primary = accentPalette.solid,
        onPrimary = LabColors.White,
        secondary = accentPalette.light,
        background = LabColors.Black,
        surface = LabColors.Black,
        surfaceVariant = LabColors.Gray,
        onBackground = LabColors.White85,
        onSurface = LabColors.White85,
        outline = LabColors.ShellBorder,
    )
    CompositionLocalProvider(
        LocalUiPreferences provides prefs,
        LocalAccentPalette provides accentPalette,
        LocalDensity provides scaledDensity,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LabTypography,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LabColors.Black),
            ) {
                content()
            }
        }
    }
}
