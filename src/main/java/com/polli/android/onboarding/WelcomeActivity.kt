package com.polli.android.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.integration.android.IntentIntegrator
import com.polli.android.BaseComposeActivity
import com.polli.android.navigation.AppNav
import com.polli.android.permissions.BackgroundSetup
import com.polli.android.qr.QrResultHandler
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabTheme
import com.polli.android.ui.AppInsets
import com.polli.ui.screens.WelcomeScreen
import androidx.compose.ui.unit.dp
import org.thoughtcrime.securesms.WelcomeActivity as DcWelcomeActivity
import org.thoughtcrime.securesms.qr.RegistrationQrActivity

class WelcomeActivity : BaseComposeActivity() {
    private val scanQr = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        val raw = data.getStringExtra(RegistrationQrActivity.QRDATA_EXTRA)
            ?: IntentIntegrator.parseActivityResult(result.resultCode, data)?.contents
        if (!raw.isNullOrBlank()) {
            QrResultHandler.handle(this, raw)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = AppPrefs(this)
        setContent {
            LabTheme(prefs = prefs) {
                WelcomeScreen(
                    topInset = AppInsets.statusBarTop(),
                    bottomInset = AppInsets.navigationBarBottom() + 32.dp,
                    onCreateAccount = { AppNav.openAccountSetup(this) },
                    onImportQr = { AppNav.openQr(this) },
                    onLinkSecondDevice = {
                        BackgroundSetup.requestNotificationsThen(this) {
                            scanQr.launch(
                                Intent(this, RegistrationQrActivity::class.java).apply {
                                    putExtra(RegistrationQrActivity.ADD_AS_SECOND_DEVICE_EXTRA, true)
                                },
                            )
                        }
                    },
                    onAdvancedSetup = {
                        startActivity(Intent(this, DcWelcomeActivity::class.java))
                    },
                )
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, WelcomeActivity::class.java)
    }
}
