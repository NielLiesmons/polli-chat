package com.polli.android.platform

import android.content.Context
import org.thoughtcrime.securesms.util.views.ProgressDialog

object PlatformDialogs {
    fun createProgressDialog(context: Context): ProgressDialog = ProgressDialog(context)
}

typealias LegacyProgressDialog = ProgressDialog
