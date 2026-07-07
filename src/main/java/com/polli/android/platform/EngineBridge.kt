package com.polli.android.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import chat.delta.rpc.Rpc
import com.b44t.messenger.DcAccounts
import com.b44t.messenger.DcContext
import org.thoughtcrime.securesms.connect.DcEventCenter
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.notifications.NotificationCenter

/** JNI/RPC engine access — sole `DcHelper` entry from Polli UI code. */
object EngineBridge {
    const val CONFIG_CONFIGURED_ADDRESS: String = DcHelper.CONFIG_CONFIGURED_ADDRESS
    const val CONFIG_DISPLAY_NAME: String = DcHelper.CONFIG_DISPLAY_NAME
    const val CONFIG_BCC_SELF: String = DcHelper.CONFIG_BCC_SELF
    const val CONFIG_PROXY_ENABLED: String = DcHelper.CONFIG_PROXY_ENABLED
    const val CONFIG_PROXY_URL: String = DcHelper.CONFIG_PROXY_URL
    const val CONFIG_FORCE_ENCRYPTION: String = DcHelper.CONFIG_FORCE_ENCRYPTION

    fun isConfigured(context: Context): Boolean = DcHelper.isConfigured(context)

    fun getContext(context: Context): DcContext = DcHelper.getContext(context)

    fun getRpc(context: Context): Rpc = DcHelper.getRpc(context)

    fun getAccounts(context: Context): DcAccounts = DcHelper.getAccounts(context)

    fun getEventCenter(context: Context): DcEventCenter = DcHelper.getEventCenter(context)

    fun getNotificationCenter(context: Context): NotificationCenter =
        DcHelper.getNotificationCenter(context)

    fun get(context: Context, key: String): String? = DcHelper.get(context, key)

    fun getInt(context: Context, key: String): Int = DcHelper.getInt(context, key)

    fun set(context: Context, key: String, value: String) {
        DcHelper.set(context, key, value)
    }

    fun captureNextError(context: Context) {
        getEventCenter(context).captureNextError()
    }

    fun endCaptureNextError(context: Context) {
        getEventCenter(context).endCaptureNextError()
    }

    fun openForViewOrShare(context: Context, messageId: Int, action: String) {
        DcHelper.openForViewOrShare(context, messageId, action)
    }

    fun copyToBlobdir(
        context: Context,
        uri: Uri,
        filename: String?,
        ext: String?,
    ): String = DcHelper.copyToBlobdir(context, uri, filename, ext)
}
