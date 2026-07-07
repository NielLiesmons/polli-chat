package com.polli.android.platform

import android.app.Activity
import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.polli.android.qr.WifiSsid
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.util.IntentUtils
import org.thoughtcrime.securesms.util.Util

object PlatformLegacyUtil {
    fun showInBrowser(context: Context, url: String) {
        IntentUtils.showInBrowser(context, url)
    }

    fun isNetworkConnected(context: Context): Boolean = DcHelper.isNetworkConnected(context)

    fun openHelp(context: Context, section: String) = DcHelper.openHelp(context, section)

    fun maybeShowMigrationError(context: Context) = DcHelper.maybeShowMigrationError(context)

    fun runOnAnyBackgroundThread(block: () -> Unit) {
        Util.runOnAnyBackgroundThread(block)
    }

    fun redPositiveButton(dialog: AlertDialog) {
        Util.redPositiveButton(dialog)
    }

    fun appendBackupTransferSsid(activity: Activity, messageView: TextView) {
        Thread {
            val ssid = WifiSsid.current(activity)
            if (ssid != null) {
                activity.runOnUiThread { messageView.text = "${messageView.text} ($ssid)" }
            }
        }.start()
    }
}
