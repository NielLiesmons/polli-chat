package com.polli.android.onboarding

import android.app.Activity
import android.text.util.Linkify
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

object OnboardingErrors {
    @JvmStatic
    fun maybeShowConfigurationError(activity: Activity, message: String?) {
        if (activity.isFinishing) return
        if (message.isNullOrEmpty()) return
        val dialog =
            AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .create()
        dialog.show()
        try {
            Linkify.addLinks(
                dialog.findViewById(android.R.id.message) as TextView,
                Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES,
            )
        } catch (_: NullPointerException) {
            // Dialog message view missing on some OEM skins.
        }
    }
}
