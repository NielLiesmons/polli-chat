package com.polli.android.platform

import android.app.Activity
import org.thoughtcrime.securesms.geolocation.LocationSharing

object PlatformAttachments {
    fun selectLocation(activity: Activity, chatId: Int) {
        LocationSharing.selectLocation(activity, chatId)
    }
}
