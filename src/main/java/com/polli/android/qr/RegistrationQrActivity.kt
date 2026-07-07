package com.polli.android.qr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcLot
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.polli.android.BaseComposeActivity
import com.polli.android.platform.EngineBridge
import com.polli.android.platform.PlatformLegacyUtil
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import org.thoughtcrime.securesms.R

/** Compose registration / add-second-device QR scanner (keeps zxing engine). */
class RegistrationQrActivity : BaseComposeActivity() {

    private var barcodeView: DecoratedBarcodeView? = null
    private var capture: InterceptingCaptureManager? = null
    private var addAsSecondDevice = false
    private val dc: DcContext get() = EngineBridge.getContext(this)

    private var sameNetworkHint by mutableStateOf("")

    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startScanner()
        } else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addAsSecondDevice = intent.getBooleanExtra(ADD_AS_SECOND_DEVICE_EXTRA, false)

        val view = DecoratedBarcodeView(this).apply {
            setStatusText(getString(R.string.qrscan_hint) + "\n ")
        }
        barcodeView = view

        if (addAsSecondDevice) {
            sameNetworkHint = getString(R.string.multidevice_same_network_hint)
            lifecycleScope.launch {
                val ssid = withContext(Dispatchers.IO) { WifiSsid.current(this@RegistrationQrActivity) }
                if (ssid != null) sameNetworkHint = "$sameNetworkHint ($ssid)"
            }
        }

        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                RegistrationScanScreen(
                    title = getString(
                        if (addAsSecondDevice) R.string.multidevice_receiver_title else R.string.scan_invitation_code,
                    ),
                    showHints = addAsSecondDevice,
                    sameNetworkHint = sameNetworkHint,
                    barcodeView = view,
                    onBack = { finish() },
                    onPaste = { pasteFromClipboard() },
                    onTroubleshooting = if (addAsSecondDevice) {
                        { PlatformLegacyUtil.openHelp(this, "#multiclient") }
                    } else {
                        null
                    },
                )
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startScanner()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScanner() {
        val view = barcodeView ?: return
        val cap = InterceptingCaptureManager(this, view)
        cap.onFrameworkBug = { message ->
            val text = message.ifEmpty { getString(R.string.zxing_msg_camera_framework_bug) }
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
        cap.resultInterceptor = { result, proceed ->
            showConfirmDialog(
                rawQr = result.text,
                onConfirm = { proceed.run() },
                onCancel = {
                    view.resume()
                    capture?.decode()
                },
            )
        }
        cap.initializeFromIntent(intent, null)
        cap.decode()
        capture = cap
    }

    private fun pasteFromClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val rawQr = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
        if (rawQr.isEmpty()) return
        showConfirmDialog(
            rawQr = rawQr,
            onConfirm = {
                setResult(Activity.RESULT_OK, Intent().putExtra(QRDATA_EXTRA, rawQr))
                finish()
            },
            onCancel = null,
        )
    }

    private fun showConfirmDialog(rawQr: String, onConfirm: () -> Unit, onCancel: (() -> Unit)?) {
        val parsed: DcLot = dc.checkQr(rawQr)
        val message = when (parsed.state) {
            DcContext.DC_QR_ASK_VERIFYCONTACT ->
                getString(R.string.instant_onboarding_confirm_contact, dc.getContact(parsed.id).displayName)
            DcContext.DC_QR_ASK_VERIFYGROUP ->
                getString(R.string.instant_onboarding_confirm_group, parsed.text1)
            else -> null
        }
        if (message == null) {
            onConfirm()
            return
        }
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> onCancel?.invoke() }
            .show()
    }

    override fun onResume() {
        super.onResume()
        capture?.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture?.onSaveInstanceState(outState)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        barcodeView?.onKeyDown(keyCode, event) == true || super.onKeyDown(keyCode, event)

    companion object {
        const val ADD_AS_SECOND_DEVICE_EXTRA = "add_as_second_device"
        const val QRDATA_EXTRA = "qrdata"

        fun intent(context: Context): Intent = Intent(context, RegistrationQrActivity::class.java)
    }
}

@Composable
private fun RegistrationScanScreen(
    title: String,
    showHints: Boolean,
    sameNetworkHint: String,
    barcodeView: DecoratedBarcodeView,
    onBack: () -> Unit,
    onPaste: () -> Unit,
    onTroubleshooting: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .padding(top = AppInsets.statusBarTop() + 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                title,
                color = PolliColors.White85,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            RegistrationMenu(onPaste = onPaste, onTroubleshooting = onTroubleshooting)
        }

        if (showHints) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "\u2780 $sameNetworkHint",
                    color = PolliColors.White66,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "\u2781 ${stringResource(R.string.multidevice_open_settings_on_other_device)}",
                    color = PolliColors.White66,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ZxingScannerView(barcodeView = barcodeView, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun RegistrationMenu(onPaste: () -> Unit, onTroubleshooting: (() -> Unit)?) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = PolliColors.White85)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.paste_from_clipboard)) },
                onClick = { open = false; onPaste() },
            )
            if (onTroubleshooting != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.troubleshooting)) },
                    onClick = { open = false; onTroubleshooting() },
                )
            }
        }
    }
}
