package com.polli.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

/** Base for Polli Compose activities — enforces gray-mode #121212 chrome. */
abstract class BaseComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val polliBlack = Color.parseColor("#121212")
        window.statusBarColor = polliBlack
        window.navigationBarColor = polliBlack
        window.decorView.setBackgroundColor(polliBlack)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
}
