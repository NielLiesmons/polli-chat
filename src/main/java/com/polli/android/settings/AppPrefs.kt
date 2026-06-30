package com.polli.android.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Density
import com.polli.domain.prefs.AccentPreset
import com.polli.domain.prefs.UiPreferences
import com.polli.domain.prefs.UiScalePreset
import com.polli.ui.theme.polliBaselineDensity

typealias UiScalePreset = com.polli.domain.prefs.UiScalePreset
typealias AccentPreset = com.polli.domain.prefs.AccentPreset

private const val PREFS = "polli_prefs"
private const val KEY_SCALE = "ui_scale_preset"
private const val KEY_SCALE_V2 = "ui_scale_preset_v2"
private const val KEY_RESPECT_SYSTEM = "respect_system_scale"
private const val KEY_ACCENT = "accent_preset"
private const val KEY_USE_DC_HOME = "use_dc_home"
private const val KEY_SIGIL_ONLY = "sigil_only_mode"

/** Bumps Compose when device-level display prefs change outside a single screen. */
object AppSettingsNotifier {
    var generation by mutableStateOf(0)
        private set

    fun notifyChanged() {
        generation++
    }
}

class AppPrefs(context: Context) : UiPreferences {
    private val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override var uiScalePreset: UiScalePreset
        get() = readUiScalePreset()
        set(value) = sp.edit()
            .putInt(KEY_SCALE, value.ordinal)
            .putBoolean(KEY_SCALE_V2, true)
            .apply()

    private fun readUiScalePreset(): UiScalePreset {
        if (!sp.contains(KEY_SCALE)) return UiScalePreset.default
        if (!sp.getBoolean(KEY_SCALE_V2, false)) {
            val legacy = sp.getInt(KEY_SCALE, UiScalePreset.default.ordinal).coerceIn(0, 2)
            val migrated = when (legacy) {
                0 -> UiScalePreset.Small
                2 -> UiScalePreset.Large
                else -> UiScalePreset.Normal
            }
            sp.edit()
                .putInt(KEY_SCALE, migrated.ordinal)
                .putBoolean(KEY_SCALE_V2, true)
                .apply()
            return migrated
        }
        val ordinal = sp.getInt(KEY_SCALE, UiScalePreset.default.ordinal)
        return UiScalePreset.entries.getOrNull(ordinal) ?: UiScalePreset.default
    }

    override var respectSystemScale: Boolean
        get() = false
        set(_) = Unit

    override var accentPreset: AccentPreset
        get() = AccentPreset.fromId(sp.getString(KEY_ACCENT, AccentPreset.Default.id) ?: AccentPreset.Default.id)
        set(value) = sp.edit().putString(KEY_ACCENT, value.id).apply()

    /** Legacy debug flag — no longer switches launcher; kept for migration. */
    var useDcHome: Boolean
        get() = sp.getBoolean(KEY_USE_DC_HOME, false)
        set(value) = sp.edit().putBoolean(KEY_USE_DC_HOME, value).apply()

    /** When true, every avatar shows its MNS sigil instead of a profile photo. */
    var sigilOnlyMode: Boolean
        get() = sp.getBoolean(KEY_SIGIL_ONLY, false)
        set(value) {
            sp.edit().putBoolean(KEY_SIGIL_ONLY, value).apply()
            AppSettingsNotifier.notifyChanged()
        }

}

val LocalAppPrefs = staticCompositionLocalOf<AppPrefs> {

    error("AppPrefs not provided")
}

@Composable
fun rememberLabDensity(prefs: AppPrefs, uiScaleRevision: Int = 0): Density {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var resumeTick by mutableIntStateOf(0)
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val factor = androidx.compose.runtime.remember(uiScaleRevision, resumeTick, prefs.uiScalePreset) {
        prefs.effectiveScale(1f)
    }
    val baseline = androidx.compose.runtime.remember(context) {
        context.polliBaselineDensity()
    }
    return Density(baseline * factor, factor)
}
