package com.polli.android.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import com.polli.android.BaseAppCompatComposeActivity
import org.thoughtcrime.securesms.R
import com.polli.android.permissions.Permissions
import com.polli.android.preferences.NotificationsPreferenceFragment

/** Polli host for Delta Chat notification prefs (ringtone, background service, battery). */
class NotificationSettingsActivity : BaseAppCompatComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_application_preferences)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.pref_notifications)

        val notificationManager = NotificationManagerCompat.from(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationManager.areNotificationsEnabled()
        ) {
            AlertDialog.Builder(this)
                .setTitle(R.string.notifications_disabled)
                .setMessage(R.string.perm_explain_access_to_notifications_denied)
                .setPositiveButton(R.string.perm_continue) { _, _ ->
                    startActivity(Permissions.getApplicationSettingsIntent(this))
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
                .setOnDismissListener { finish() }
                .show()
            return
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment, NotificationsPreferenceFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, NotificationSettingsActivity::class.java)
    }
}
