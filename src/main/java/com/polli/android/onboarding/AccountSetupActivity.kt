package com.polli.android.onboarding

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import chat.delta.rpc.RpcException
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.polli.android.BaseAppCompatComposeActivity
import com.polli.android.navigation.AppNav
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabTheme
import com.polli.android.ui.AppInsets
import com.polli.ui.screens.AccountSetupScreen
import androidx.compose.ui.unit.dp
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.DcEventCenter
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.util.Util
import org.thoughtcrime.securesms.util.views.ProgressDialog
import java.util.concurrent.Executors

class AccountSetupActivity : BaseAppCompatComposeActivity(), DcEventCenter.DcEventDelegate {
    private val executor = Executors.newSingleThreadExecutor()
    private var progressDialog: ProgressDialog? = null
    private var cancelled = false
    private lateinit var providerQrData: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        providerQrData = intent.getStringExtra(EXTRA_QR_DATA) ?: DEFAULT_PROVIDER_QR
        if (DcHelper.getContext(this).isConfigured == 1) {
            finish()
            return
        }
        DcHelper.getEventCenter(this).addObserver(DcContext.DC_EVENT_CONFIGURE_PROGRESS, this)
        val prefs = AppPrefs(this)
        setContent {
            LabTheme(prefs = prefs) {
                AccountSetupScreen(
                    initialDisplayName = DcHelper.get(this, DcHelper.CONFIG_DISPLAY_NAME).orEmpty(),
                    topInset = AppInsets.statusBarTop(),
                    bottomInset = AppInsets.navigationBarBottom() + 24.dp,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onCreate = { name -> createProfile(name) },
                )
            }
        }
    }

    override fun onDestroy() {
        DcHelper.getEventCenter(this).removeObservers(this)
        executor.shutdown()
        super.onDestroy()
    }

    override fun handleEvent(event: DcEvent) {
        if (event.id == DcContext.DC_EVENT_CONFIGURE_PROGRESS) {
            val percent = event.data1Int / 10
            progressDialog?.setMessage(getString(R.string.one_moment) + " $percent%")
        }
    }

    private fun createProfile(name: String) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, R.string.please_enter_name, Toast.LENGTH_LONG).show()
            return
        }
        executor.execute {
            DcHelper.set(this, DcHelper.CONFIG_DISPLAY_NAME, name)
            Util.runOnMain { startQrAccountCreation(providerQrData) }
        }
    }

    private fun startQrAccountCreation(qrCode: String) {
        progressDialog?.dismiss()
        cancelled = false
        progressDialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.one_moment))
            setCanceledOnTouchOutside(false)
            setCancelable(false)
            setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel)) { _, _ ->
                cancelled = true
                DcHelper.getContext(this@AccountSetupActivity).stopOngoingProcess()
            }
            show()
        }
        DcHelper.getEventCenter(this).captureNextError()
        Thread {
            val dc = DcHelper.getContext(this)
            try {
                DcHelper.getRpc(this).addTransportFromQr(dc.accountId, qrCode)
                DcHelper.getEventCenter(this).endCaptureNextError()
                Util.runOnMain {
                    progressDialog?.dismiss()
                    startActivity(AppNav.homeIntentFromWelcome(this))
                    finishAffinity()
                }
            } catch (e: RpcException) {
                DcHelper.getEventCenter(this).endCaptureNextError()
                if (!cancelled) {
                    Util.runOnMain {
                        progressDialog?.dismiss()
                        Toast.makeText(this, e.message ?: getString(R.string.error), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }.start()
    }

    companion object {
        const val EXTRA_QR_DATA = "qr_data"
        const val DEFAULT_PROVIDER_QR = "dcaccount:nine.testrun.org"

        fun intent(context: android.content.Context): Intent =
            Intent(context, AccountSetupActivity::class.java)
    }
}
