package com.polli.android.platform

import android.app.Activity
import org.thoughtcrime.securesms.mms.AttachmentManager

object PlatformAttachments {
    fun selectLocation(activity: Activity, chatId: Int) {
        AttachmentManager.selectLocation(activity, chatId)
    }
}
