package com.polli.android.qr

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcLot
import com.polli.android.navigation.AppNav
import com.polli.android.onboarding.AdvancedOnboardingActivity
import com.polli.android.onboarding.AccountSetupActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.AccountManager
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.qr.BackupTransferActivity

object QrResultHandler {
    fun handle(activity: Activity, rawQr: String) {
        val trimmed = rawQr.trim()
        if (trimmed.isEmpty()) return
        val dc = DcHelper.getContext(activity)
        val parsed = dc.checkQr(trimmed)
        when (parsed.state) {
            DcContext.DC_QR_LOGIN, DcContext.DC_QR_ACCOUNT -> {
                activity.startActivity(
                    AccountSetupActivity.intent(activity).apply {
                        putExtra(AccountSetupActivity.EXTRA_QR_DATA, trimmed)
                    },
                )
            }
            DcContext.DC_QR_ASK_VERIFYCONTACT, DcContext.DC_QR_ASK_VERIFYGROUP -> {
                activity.startActivity(AdvancedOnboardingActivity.intentWithQr(activity, trimmed))
            }
            DcContext.DC_QR_BACKUP2 -> showBackup2Dialog(activity, trimmed)
            DcContext.DC_QR_BACKUP_TOO_NEW -> {
                AlertDialog.Builder(activity)
                    .setTitle(R.string.multidevice_receiver_title)
                    .setMessage(activity.getString(R.string.multidevice_receiver_needs_update))
                    .setNegativeButton(R.string.ok, null)
                    .show()
            }
            else -> Toast.makeText(activity, R.string.qraccount_qr_code_cannot_be_used, Toast.LENGTH_LONG).show()
        }
    }

    fun handleLot(context: Context, parsed: DcLot, rawQr: String) {
        if (context is Activity) {
            when (parsed.state) {
                DcContext.DC_QR_LOGIN, DcContext.DC_QR_ACCOUNT -> {
                    context.startActivity(
                        AccountSetupActivity.intent(context).apply {
                            putExtra(AccountSetupActivity.EXTRA_QR_DATA, rawQr.trim())
                        },
                    )
                }
                else -> handle(context, rawQr)
            }
        }
    }

    private fun showBackup2Dialog(activity: Activity, rawQr: String) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.multidevice_receiver_title)
            .setMessage(
                activity.getString(R.string.multidevice_receiver_scanning_ask) +
                    "\n\n" +
                    activity.getString(R.string.multidevice_same_network_hint),
            )
            .setPositiveButton(R.string.perm_continue) { _, _ ->
                AccountManager.getInstance().addAccountFromSecondDevice(activity, rawQr)
            }
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(false)
            .create()
        dialog.show()
        BackupTransferActivity.appendSSID(activity, dialog.findViewById(android.R.id.message))
    }
}
