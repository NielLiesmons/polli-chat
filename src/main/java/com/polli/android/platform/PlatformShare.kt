package com.polli.android.platform

import android.app.Activity
import android.content.Intent
import org.thoughtcrime.securesms.util.SendRelayedMessageUtil
import org.thoughtcrime.securesms.util.ShareUtil

object PlatformShare {
    fun isForwarding(activity: Activity): Boolean = ShareUtil.isForwarding(activity)

    fun isSharing(activity: Activity): Boolean = ShareUtil.isSharing(activity)

    fun getForwardedMessageIds(activity: Activity): IntArray? = ShareUtil.getForwardedMessageIDs(activity)

    fun getSharedUris(activity: Activity): List<android.net.Uri> = ShareUtil.getSharedUris(activity)

    fun getSharedText(activity: Activity): String? = ShareUtil.getSharedText(activity)

    fun relayImmediately(activity: Activity, chatId: Int) {
        SendRelayedMessageUtil.immediatelyRelay(activity, chatId)
    }

    fun setForwardingMessageIds(intent: Intent, msgIds: IntArray, accountId: Int) {
        ShareUtil.setForwardingMessageIds(intent, msgIds, accountId)
    }

    fun isRelayingMessageContent(activity: Activity): Boolean =
        ShareUtil.isRelayingMessageContent(activity)

    fun acquireRelayMessageContent(activity: Activity, intent: Intent) {
        ShareUtil.acquireRelayMessageContent(activity, intent)
    }
}
