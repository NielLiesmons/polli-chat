package com.polli.android.platform

import android.app.Activity
import com.polli.android.connect.AccountManager

object PlatformAccounts {
    fun addAccountFromSecondDevice(activity: Activity, rawQr: String) {
        AccountManager.getInstance().addAccountFromSecondDevice(activity, rawQr)
    }
}
