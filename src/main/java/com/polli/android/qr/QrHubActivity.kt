package com.polli.android.qr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import com.google.zxing.integration.android.IntentIntegrator
import com.polli.android.BaseAppCompatComposeActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.qr.QrShowFragment
import org.thoughtcrime.securesms.qr.RegistrationQrActivity

class QrHubActivity : BaseAppCompatComposeActivity() {
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
                QrHubScreen(
                    onBack = { finish() },
                    onScan = { launchScan() },
                )
            }
        }
    }

    private fun launchScan() {
        scanQr.launch(Intent(this, RegistrationQrActivity::class.java))
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, QrHubActivity::class.java)
    }
}

@Composable
fun QrHubScreen(onBack: () -> Unit, onScan: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val dc = remember { DcHelper.getContext(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.Black),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppInsets.statusBarTop() + 8.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBackButton(onClick = onBack)
            Text(
                "QR code",
                color = LabColors.White85,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        ScrollableTabRow(
            selectedTabIndex = tab,
            containerColor = LabColors.Black,
            contentColor = LabColors.White85,
            edgePadding = 16.dp,
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Show") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Scan") })
        }

        when (tab) {
            0 -> {
                val svg = remember {
                    runCatching { QrShowFragment.fixSVG(dc.getSecurejoinQrSvg(0)) }.getOrNull()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (svg != null) {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { ctx -> SVGImageView(ctx) },
                            update = { view ->
                                runCatching { view.setSVG(SVG.getFromString(svg)) }
                            },
                        )
                    } else {
                        Text("Unable to load QR", color = LabColors.White33)
                    }
                }
            }
            1 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = onScan) {
                        Text("Open scanner", color = LabColors.White)
                    }
                }
            }
        }
    }
}
