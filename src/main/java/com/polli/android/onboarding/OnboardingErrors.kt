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
            val messageView = dialog.findViewById<TextView>(android.R.id.message)
            if (messageView != null) {
                Linkify.addLinks(messageView, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
            }
        } catch (_: Exception) {
            // Dialog message view missing on some OEM skins.
        }
    }
}
