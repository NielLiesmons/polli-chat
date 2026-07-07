package com.polli.android.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import com.b44t.messenger.DcContact
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcMsg
import chat.delta.rpc.types.SecurejoinSource
import chat.delta.rpc.types.SecurejoinUiPath
import com.polli.domain.navigation.ChatIntentExtras
import com.polli.android.R
import com.polli.android.components.reminder.DozeReminder
import com.polli.android.platform.EngineBridge
import com.polli.android.notifications.FcmReceiveService
import com.polli.android.permissions.Permissions
import com.polli.android.qr.QrCodeHandler
import com.polli.android.platform.PlatformPrefs

/**
 * Delta Chat background + permission prompts from [org.thoughtcrime.securesms.ConversationListFragment]
 * and [org.thoughtcrime.securesms.ConversationListActivity], wired into the Polli Compose home path.
 */
object BackgroundSetup {

    /** Call from [com.polli.android.HomeActivity.onCreate] — welcome device msgs + join-by-QR. */
    @JvmStatic
    fun handleHomeIntent(activity: Activity, intent: Intent) {
        addDeviceMessages(activity, intent.getBooleanExtra(ChatIntentExtras.FROM_WELCOME, false))
        intent.getStringExtra(ChatIntentExtras.FROM_WELCOME_RAW_QR)?.let { rawQr ->
            QrCodeHandler(activity).secureJoinByQr(rawQr, SecurejoinSource.Scan, SecurejoinUiPath.Unknown)
            intent.removeExtra(ChatIntentExtras.FROM_WELCOME_RAW_QR)
        }
        if (intent.getBooleanExtra(ChatIntentExtras.FROM_WELCOME, false)) {
            intent.removeExtra(ChatIntentExtras.FROM_WELCOME)
        }
    }

    /**
     * Call from home / inbox [Activity.onResume] — POST_NOTIFICATIONS, battery optimization,
     * and optional doze device message (same as DC [updateReminders]).
     */
    @JvmStatic
    fun updateReminders(activity: Activity) {
        @Suppress("DEPRECATION")
        object : AsyncTask<Context, Void, Void>() {
            override fun doInBackground(vararg params: Context): Void? {
                val context = params[0]
                try {
                    if (DozeReminder.isEligible(context)) {
                        DozeReminder.addDozeReminderDeviceMsg(context)
                    }
                    FcmReceiveService.waitForRegisterFinished()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return null
            }

            override fun onPostExecute(result: Void?) {
                if (activity.isFinishing) return
                promptNotificationsAndBattery(activity)
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, activity)
    }

    /** One-shot before second-device QR (DC [WelcomeActivity.showSignInDialogWithPermission]). */
    @JvmStatic
    fun requestNotificationsThen(activity: Activity, onContinue: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PlatformPrefs.getBooleanPreference(activity, PlatformPrefs.askedForNotificationPermission, false)
        ) {
            PlatformPrefs.setBooleanPreference(activity, PlatformPrefs.askedForNotificationPermission, true)
            Permissions.with(activity)
                .request(Manifest.permission.POST_NOTIFICATIONS)
                .ifNecessary()
                .onAllGranted { onContinue() }
                .onAnyDenied { onContinue() }
                .execute()
        } else {
            onContinue()
        }
    }

  /** Device-chat taps (battery reminder) — DC [ConversationFragment] path. */
    @JvmStatic
    fun tryHandleDeviceMessageTap(context: Context, msgId: Int): Boolean {
        val dc = EngineBridge.getContext(context)
        val msg = dc.getMsg(msgId)
        if (!msg.isOk) return false
        return if (DozeReminder.isDozeReminderMsg(context, msg)) {
            DozeReminder.dozeReminderTapped(context)
            true
        } else {
            false
        }
    }

    private fun promptNotificationsAndBattery(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PlatformPrefs.getBooleanPreference(activity, PlatformPrefs.askedForNotificationPermission, false)) {
                PlatformPrefs.setBooleanPreference(activity, PlatformPrefs.askedForNotificationPermission, true)
                Permissions.with(activity)
                    .request(Manifest.permission.POST_NOTIFICATIONS)
                    .ifNecessary()
                    .onAllGranted { DozeReminder.maybeAskDirectly(activity) }
                    .onAnyDenied {
                        val dcContext = EngineBridge.getContext(activity)
                        val msg = DcMsg(dcContext, DcMsg.DC_MSG_TEXT)
                        msg.setText(
                            "\uD83D\uDC49 ${activity.getString(R.string.notifications_disabled)} \uD83D\uDC48\n\n" +
                                activity.getString(R.string.perm_explain_access_to_notifications_denied),
                        )
                        dcContext.addDeviceMsg("android.notifications-disabled", msg)
                    }
                    .execute()
            } else {
                DozeReminder.maybeAskDirectly(activity)
            }
        } else {
            DozeReminder.maybeAskDirectly(activity)
        }
    }

    private fun addDeviceMessages(activity: Activity, fromWelcome: Boolean) {
        try {
            val dcContext = EngineBridge.getContext(activity)
            val deviceMsgLabel = "update_2_0_0_android-h"
            if (!dcContext.wasDeviceMsgEverAdded(deviceMsgLabel)) {
                val msg = if (!fromWelcome) {
                    DcMsg(dcContext, DcMsg.DC_MSG_TEXT).apply {
                        setText(activity.getString(R.string.update_2_0, "https://delta.chat/donate"))
                    }
                } else {
                    null
                }
                dcContext.addDeviceMsg(deviceMsgLabel, msg)
                if (PlatformPrefs.getStringPreference(activity, PlatformPrefs.lastDeviceMsgLabel, "") == deviceMsgLabel) {
                    val deviceChatId = dcContext.getChatIdByContactId(DcContact.DC_CONTACT_ID_DEVICE)
                    if (deviceChatId != 0) {
                        dcContext.marknoticedChat(deviceChatId)
                    }
                }
                PlatformPrefs.setStringPreference(activity, PlatformPrefs.lastDeviceMsgLabel, deviceMsgLabel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
