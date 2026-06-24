package com.polli.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.polli.domain.prefs.UiPreferences

private val LabColorScheme = darkColorScheme(
    primary = LabColors.Blurple,
    onPrimary = LabColors.White,
    secondary = LabColors.BlurpleLight,
    background = LabColors.Black,
    surface = LabColors.Black,
    surfaceVariant = LabColors.Gray,
    onBackground = LabColors.White85,
    onSurface = LabColors.White85,
    outline = LabColors.ShellBorder,
)

val LocalUiPreferences = staticCompositionLocalOf<UiPreferences> {
    error("UiPreferences not provided")
}

@Composable
fun LabTheme(
    prefs: UiPreferences,
    uiScaleRevision: Int = 0,
    content: @Composable () -> Unit,
) {
    val scaledDensity = rememberScaledDensity(prefs, uiScaleRevision)
    CompositionLocalProvider(
        LocalUiPreferences provides prefs,
        LocalDensity provides scaledDensity,
    ) {
        MaterialTheme(
            colorScheme = LabColorScheme,
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
