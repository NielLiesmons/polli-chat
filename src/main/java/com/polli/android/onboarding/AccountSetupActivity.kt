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
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.ui.screens.AccountSetupScreen
import androidx.compose.ui.unit.dp
import com.polli.android.platform.EngineBridge
import com.polli.android.platform.PlatformDialogs
import com.polli.android.platform.PlatformThread
import com.polli.android.platform.LegacyProgressDialog
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.DcEventCenter
import java.util.concurrent.Executors

class AccountSetupActivity : BaseAppCompatComposeActivity(), DcEventCenter.DcEventDelegate {
    private val executor = Executors.newSingleThreadExecutor()
    private var progressDialog: LegacyProgressDialog? = null
    private var cancelled = false
    private lateinit var providerQrData: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        providerQrData = intent.getStringExtra(EXTRA_QR_DATA) ?: DEFAULT_PROVIDER_QR
        if (EngineBridge.getContext(this).isConfigured == 1) {
            finish()
            return
        }
        EngineBridge.getEventCenter(this).addObserver(DcContext.DC_EVENT_CONFIGURE_PROGRESS, this)
        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                AccountSetupScreen(
                    initialDisplayName = EngineBridge.get(this, EngineBridge.CONFIG_DISPLAY_NAME).orEmpty(),
                    topInset = AppInsets.statusBarTop(),
                    bottomInset = AppInsets.navigationBarBottom() + 24.dp,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onCreate = { name -> createProfile(name) },
                )
            }
        }
    }

    override fun onDestroy() {
        EngineBridge.getEventCenter(this).removeObservers(this)
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
            EngineBridge.set(this, EngineBridge.CONFIG_DISPLAY_NAME, name)
            PlatformThread.runOnMain { startQrAccountCreation(providerQrData) }
        }
    }

    private fun startQrAccountCreation(qrCode: String) {
        progressDialog?.dismiss()
        cancelled = false
        progressDialog = PlatformDialogs.createProgressDialog(this).apply {
            setMessage(getString(R.string.one_moment))
            setCanceledOnTouchOutside(false)
            setCancelable(false)
            setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel)) { _, _ ->
                cancelled = true
                EngineBridge.getContext(this@AccountSetupActivity).stopOngoingProcess()
            }
            show()
        }
        EngineBridge.captureNextError(this)
        Thread {
            val dc = EngineBridge.getContext(this)
            try {
                EngineBridge.getRpc(this).addTransportFromQr(dc.accountId, qrCode)
                EngineBridge.endCaptureNextError(this)
                PlatformThread.runOnMain {
                    progressDialog?.dismiss()
                    startActivity(AppNav.homeIntentFromWelcome(this))
                    finishAffinity()
                }
            } catch (e: RpcException) {
                EngineBridge.endCaptureNextError(this)
                if (!cancelled) {
                    PlatformThread.runOnMain {
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
