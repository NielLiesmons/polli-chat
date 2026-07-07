package com.polli.android.platform

import android.app.Activity
import com.polli.android.geolocation.LocationSharing

object PlatformAttachments {
    fun selectLocation(activity: Activity, chatId: Int) {
        LocationSharing.selectLocation(activity, chatId)
    }
}
