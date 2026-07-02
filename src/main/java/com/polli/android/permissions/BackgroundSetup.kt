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
import org.thoughtcrime.securesms.ConversationListActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.reminder.DozeReminder
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.notifications.FcmReceiveService
import org.thoughtcrime.securesms.permissions.Permissions
import org.thoughtcrime.securesms.qr.QrCodeHandler
import org.thoughtcrime.securesms.util.Prefs

/**
 * Delta Chat background + permission prompts from [org.thoughtcrime.securesms.ConversationListFragment]
 * and [org.thoughtcrime.securesms.ConversationListActivity], wired into the Polli Compose home path.
 */
object BackgroundSetup {

    /** Call from [com.polli.android.HomeActivity.onCreate] — welcome device msgs + join-by-QR. */
    @JvmStatic
    fun handleHomeIntent(activity: Activity, intent: Intent) {
        addDeviceMessages(activity, intent.getBooleanExtra(ConversationListActivity.FROM_WELCOME, false))
        intent.getStringExtra(ConversationListActivity.FROM_WELCOME_RAW_QR)?.let { rawQr ->
            QrCodeHandler(activity).secureJoinByQr(rawQr, SecurejoinSource.Scan, SecurejoinUiPath.Unknown)
            intent.removeExtra(ConversationListActivity.FROM_WELCOME_RAW_QR)
        }
        if (intent.getBooleanExtra(ConversationListActivity.FROM_WELCOME, false)) {
            intent.removeExtra(ConversationListActivity.FROM_WELCOME)
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
            !Prefs.getBooleanPreference(activity, Prefs.ASKED_FOR_NOTIFICATION_PERMISSION, false)
        ) {
            Prefs.setBooleanPreference(activity, Prefs.ASKED_FOR_NOTIFICATION_PERMISSION, true)
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
        val dc = DcHelper.getContext(context)
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
            if (!Prefs.getBooleanPreference(activity, Prefs.ASKED_FOR_NOTIFICATION_PERMISSION, false)) {
                Prefs.setBooleanPreference(activity, Prefs.ASKED_FOR_NOTIFICATION_PERMISSION, true)
                Permissions.with(activity)
                    .request(Manifest.permission.POST_NOTIFICATIONS)
                    .ifNecessary()
                    .onAllGranted { DozeReminder.maybeAskDirectly(activity) }
                    .onAnyDenied {
                        val dcContext = DcHelper.getContext(activity)
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
            val dcContext = DcHelper.getContext(activity)
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
                if (Prefs.getStringPreference(activity, Prefs.LAST_DEVICE_MSG_LABEL, "") == deviceMsgLabel) {
                    val deviceChatId = dcContext.getChatIdByContactId(DcContact.DC_CONTACT_ID_DEVICE)
                    if (deviceChatId != 0) {
                        dcContext.marknoticedChat(deviceChatId)
                    }
                }
                Prefs.setStringPreference(activity, Prefs.LAST_DEVICE_MSG_LABEL, deviceMsgLabel)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
