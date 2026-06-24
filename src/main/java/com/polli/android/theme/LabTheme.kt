package com.polli.android.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.polli.android.settings.LocalAppPrefs
import com.polli.android.settings.AppPrefs
import com.polli.android.settings.rememberLabDensity

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

@Composable
fun LabTheme(
    prefs: AppPrefs,
    uiScaleRevision: Int = 0,
    content: @Composable () -> Unit,
) {
    val scaledDensity = rememberLabDensity(prefs, uiScaleRevision)
    CompositionLocalProvider(
        LocalAppPrefs provides prefs,
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
