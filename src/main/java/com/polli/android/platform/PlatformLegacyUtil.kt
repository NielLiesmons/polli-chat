package com.polli.android.platform

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.polli.android.qr.WifiSsid
import com.polli.android.connect.DcHelper
import com.polli.android.notifications.FcmReceiveService
import com.polli.android.util.FileProviderUtil
import com.polli.android.util.IntentUtils
import com.polli.android.util.Prefs
import com.polli.android.util.Util
import java.io.File

object PlatformLegacyUtil {
    fun showInBrowser(context: Context, url: String) {
        IntentUtils.showInBrowser(context, url)
    }

    fun isNetworkConnected(context: Context): Boolean = DcHelper.isNetworkConnected(context)

    fun openHelp(context: Context, section: String) = DcHelper.openHelp(context, section)

    fun maybeShowMigrationError(context: Context) = DcHelper.maybeShowMigrationError(context)

    fun reliableService(context: Context): Boolean = Prefs.reliableService(context)

    fun isPushEnabled(context: Context): Boolean = Prefs.isPushEnabled(context)

    fun pushToken(): String? = FcmReceiveService.getToken()

    fun fileProviderUri(context: Context, file: File): Uri = FileProviderUtil.getUriFor(context, file)

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
