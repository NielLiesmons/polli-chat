package com.polli.android.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.integration.android.IntentIntegrator
import com.polli.android.BaseComposeActivity
import com.polli.android.qr.QrResultHandler
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabColors
import com.polli.android.theme.accent
import com.polli.android.theme.LabTheme
import com.polli.android.ui.AppInsets
import com.polli.android.navigation.AppNav
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
                    onCreateAccount = { AppNav.openAccountSetup(this) },
                    onImportQr = { AppNav.openQr(this) },
                    onLinkSecondDevice = {
                        scanQr.launch(
                            Intent(this, RegistrationQrActivity::class.java).apply {
                                putExtra(RegistrationQrActivity.ADD_AS_SECOND_DEVICE_EXTRA, true)
                            },
                        )
                    },
                    onLegacyWelcome = {
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

@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onImportQr: () -> Unit,
    onLinkSecondDevice: () -> Unit,
    onLegacyWelcome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.Black)
            .padding(top = AppInsets.statusBarTop())
            .padding(horizontal = 24.dp)
            .padding(bottom = AppInsets.navigationBarBottom() + 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Polli",
            color = LabColors.White,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.padding(12.dp))
        Text(
            "Secure messaging for everyone",
            color = LabColors.White33,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.padding(32.dp))
        WelcomeButton("Create new account", onCreateAccount)
        Spacer(modifier = Modifier.padding(8.dp))
        WelcomeButton("Scan QR / import", onImportQr)
        Spacer(modifier = Modifier.padding(8.dp))
        WelcomeButton("Link second device", onLinkSecondDevice)
        Spacer(modifier = Modifier.padding(8.dp))
        TextButton(onClick = onLegacyWelcome) {
            Text("Advanced setup", color = LabColors.White33)
        }
    }
}

@Composable
private fun WelcomeButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(accent().solid),
    ) {
        Text(label, color = LabColors.White)
    }
}
