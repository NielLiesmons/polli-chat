package com.polli.android.platform

import android.content.Context
import android.net.Uri
import com.polli.android.util.Prefs

object PlatformPrefs {
    val askedForNotificationPermission: String = Prefs.ASKED_FOR_NOTIFICATION_PERMISSION
    val lastDeviceMsgLabel: String = Prefs.LAST_DEVICE_MSG_LABEL

    fun getBooleanPreference(context: Context, key: String, default: Boolean): Boolean =
        Prefs.getBooleanPreference(context, key, default)

    fun setBooleanPreference(context: Context, key: String, value: Boolean) {
        Prefs.setBooleanPreference(context, key, value)
    }

    fun getStringPreference(context: Context, key: String, default: String): String =
        Prefs.getStringPreference(context, key, default)

    fun setStringPreference(context: Context, key: String, value: String) {
        Prefs.setStringPreference(context, key, value)
    }

    fun setProfileAvatarId(context: Context, id: Int) {
        Prefs.setProfileAvatarId(context, id)
    }

    fun getNotificationRingtone(context: Context): Uri? = Prefs.getNotificationRingtone(context)

    fun getChatRingtone(context: Context, accountId: Int, chatId: Int): Uri? =
        Prefs.getChatRingtone(context, accountId, chatId)

    fun setChatRingtone(context: Context, accountId: Int, chatId: Int, uri: Uri?) {
        Prefs.setChatRingtone(context, accountId, chatId, uri)
    }

    fun getChatVibrate(context: Context, accountId: Int, chatId: Int): Prefs.VibrateState =
        Prefs.getChatVibrate(context, accountId, chatId)

    fun setChatVibrate(
        context: Context,
        accountId: Int,
        chatId: Int,
        state: Prefs.VibrateState,
    ) {
        Prefs.setChatVibrate(context, accountId, chatId, state)
    }

    fun vibrateStateFromId(id: Int): Prefs.VibrateState = Prefs.VibrateState.fromId(id)
}
