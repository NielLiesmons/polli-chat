package com.polli.android.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private const val PREFS = "polli_prefs"
private const val KEY_SCALE = "ui_scale_preset"
private const val KEY_RESPECT_SYSTEM = "respect_system_scale"
private const val KEY_USE_DC_HOME = "use_dc_home"

enum class UiScalePreset(val factor: Float, val label: String) {
    Small(0.925f, "Small"),
    Normal(1.0f, "Normal"),
    Large(1.075f, "Large"),
}

class AppPrefs(context: Context) {
    private val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var uiScalePreset: UiScalePreset
        get() = UiScalePreset.entries.getOrElse(sp.getInt(KEY_SCALE, 1)) { UiScalePreset.Normal }
        set(value) = sp.edit().putInt(KEY_SCALE, value.ordinal).apply()

    var respectSystemScale: Boolean
        get() = sp.getBoolean(KEY_RESPECT_SYSTEM, true)
        set(value) = sp.edit().putBoolean(KEY_RESPECT_SYSTEM, value).apply()

    /** Legacy debug flag — no longer switches launcher; kept for migration. */
    var useDcHome: Boolean
        get() = sp.getBoolean(KEY_USE_DC_HOME, false)
        set(value) = sp.edit().putBoolean(KEY_USE_DC_HOME, value).apply()

    fun effectiveScale(systemFontScale: Float): Float {
        val base = uiScalePreset.factor
        return if (respectSystemScale) {
            (base * systemFontScale).coerceIn(0.8f, 1.35f)
        } else {
            base
        }
    }
}

val LocalAppPrefs = staticCompositionLocalOf<AppPrefs> {
    error("AppPrefs not provided")
}

@Composable
fun rememberLabDensity(prefs: AppPrefs, uiScaleRevision: Int = 0): Density {
    val base = LocalDensity.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var resumeTick by mutableIntStateOf(0)
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val systemScale = LocalConfiguration.current.fontScale
    val factor = androidx.compose.runtime.remember(uiScaleRevision, resumeTick, systemScale, prefs.uiScalePreset, prefs.respectSystemScale) {
        prefs.effectiveScale(systemScale)
    }
    return Density(base.density * factor, base.fontScale * factor)
}
