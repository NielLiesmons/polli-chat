package com.polli.android.qr

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcLot
import com.polli.android.navigation.AppNav
import com.polli.android.onboarding.AccountSetupActivity
import org.thoughtcrime.securesms.InstantOnboardingActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.DcHelper

object QrResultHandler {
  fun handle(activity: Activity, rawQr: String) {
    val dc = DcHelper.getContext(activity)
    val parsed = dc.checkQr(rawQr)
    when (parsed.state) {
      DcContext.DC_QR_LOGIN, DcContext.DC_QR_ACCOUNT -> {
        activity.startActivity(
          AccountSetupActivity.intent(activity).apply {
            putExtra(AccountSetupActivity.EXTRA_QR_DATA, rawQr)
          },
        )
      }
      DcContext.DC_QR_ASK_VERIFYCONTACT, DcContext.DC_QR_ASK_VERIFYGROUP -> {
        activity.startActivity(Intent(activity, InstantOnboardingActivity::class.java).apply {
          data = android.net.Uri.parse(rawQr)
        })
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
              putExtra(AccountSetupActivity.EXTRA_QR_DATA, rawQr)
            },
          )
        }
        else -> handle(context, rawQr)
      }
    }
  }
}
