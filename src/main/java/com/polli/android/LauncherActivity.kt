package com.polli.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.polli.android.navigation.AppNav

/** Routes the launcher intent to Polli home or classic Delta Chat list. */
class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launch = Intent(this, AppNav.homeActivityClass()).apply {
            action = intent.action
            data = intent.data
            intent.categories?.forEach { addCategory(it) }
            flags = intent.flags
            intent.extras?.let { putExtras(it) }
        }
        startActivity(launch)
        finish()
    }
}
