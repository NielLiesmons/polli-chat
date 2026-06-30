package com.polli.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics

/**
 * Strip system font-size and display-size overrides so only the in-app UI scale preset applies.
 * Used from [rememberScaledDensity] and [wrapPolliUiContext] (Application.attachBaseContext).
 */
fun Context.polliBaselineDensity(): Float {
    val configuration = Configuration(resources.configuration)
    configuration.fontScale = 1f
    val stableDpi = displayStableDensityDpi()
    if (stableDpi > 0) {
        configuration.densityDpi = stableDpi
    }
    return createConfigurationContext(configuration).resources.displayMetrics.density
}

fun wrapPolliUiContext(base: Context): Context {
    val configuration = Configuration(base.resources.configuration)
    configuration.fontScale = 1f
    val stableDpi = base.displayStableDensityDpi()
    if (stableDpi > 0) {
        configuration.densityDpi = stableDpi
    }
    return base.createConfigurationContext(configuration)
}

private fun Context.displayStableDensityDpi(): Int {
    val metrics = resources.displayMetrics
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return try {
            val field = DisplayMetrics::class.java.getDeclaredField("noncompatDensityDpi")
            field.isAccessible = true
            field.getInt(metrics).takeIf { it > 0 } ?: metrics.densityDpi
        } catch (_: ReflectiveOperationException) {
            metrics.densityDpi
        }
    }
    return metrics.densityDpi
}
