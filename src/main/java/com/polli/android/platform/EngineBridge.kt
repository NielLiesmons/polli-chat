package com.polli.android.platform

import android.content.Context
import chat.delta.rpc.Rpc
import com.b44t.messenger.DcContext
import org.thoughtcrime.securesms.connect.DcEventCenter
import org.thoughtcrime.securesms.connect.DcHelper

/** JNI/RPC engine access — sole `DcHelper` entry from Polli UI code. */
object EngineBridge {
    const val CONFIG_DISPLAY_NAME: String = DcHelper.CONFIG_DISPLAY_NAME
    const val CONFIG_CONFIGURED_ADDRESS: String = DcHelper.CONFIG_CONFIGURED_ADDRESS

    fun isConfigured(context: Context): Boolean = DcHelper.isConfigured(context)

    fun getContext(context: Context): DcContext = DcHelper.getContext(context)

    fun getRpc(context: Context): Rpc = DcHelper.getRpc(context)

    fun getEventCenter(context: Context): DcEventCenter = DcHelper.getEventCenter(context)
}
