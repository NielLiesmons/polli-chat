package com.polli.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

/** AppCompat base for Compose screens that need fragment / legacy interop. */
abstract class BaseAppCompatComposeActivity : AppCompatActivity() {
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
