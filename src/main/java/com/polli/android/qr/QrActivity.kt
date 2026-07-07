package com.polli.android.qr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.client.android.Intents
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.polli.android.BaseComposeActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import com.polli.android.R

/** Scanner-only QR capture activity (zxing engine hosted in Compose). Returns [Intents.Scan.RESULT]. */
class QrActivity : BaseComposeActivity() {

    private var barcodeView: DecoratedBarcodeView? = null
    private var capture: InterceptingCaptureManager? = null
    private var scanRelay = false

    private val requestCamera = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) startScanner() else onCameraDenied() }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { decodeImage(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanRelay = intent.getBooleanExtra(EXTRA_SCAN_RELAY, false)

        val view = DecoratedBarcodeView(this).apply {
            setStatusText(getString(R.string.qrscan_hint) + "\n ")
        }
        barcodeView = view

        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                ScannerScreen(
                    title = getString(if (scanRelay) R.string.add_transport else R.string.qrscan_title),
                    barcodeView = view,
                    onBack = { finish() },
                    onLoadImage = { pickImage.launch("image/*") },
                    onPaste = { pasteFromClipboard() },
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
        cap.initializeFromIntent(intent, null)
        cap.decode()
        capture = cap
    }

    private fun onCameraDenied() {
        Toast.makeText(this, getString(R.string.perm_explain_access_to_camera_denied), Toast.LENGTH_LONG).show()
        finish()
    }

    private fun pasteFromClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) setQrResult(text)
    }

    private fun decodeImage(uri: Uri) {
        try {
            val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            if (bitmap == null) {
                Log.e(TAG, "uri is not a bitmap: $uri")
                return
            }
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.recycle()
            val source = RGBLuminanceSource(width, height, pixels)
            val binary = BinaryBitmap(HybridBinarizer(source))
            try {
                val result = MultiFormatReader().decode(binary)
                setQrResult(result.text)
            } catch (e: NotFoundException) {
                Log.e(TAG, "decode exception", e)
                Toast.makeText(this, getString(R.string.qrscan_failed), Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "can not open file: $uri", e)
        }
    }

    private fun setQrResult(qrData: String) {
        val data = Intent().putExtra(Intents.Scan.RESULT, qrData)
        setResult(Activity.RESULT_OK, data)
        finish()
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
        private const val TAG = "QrActivity"
        const val EXTRA_SCAN_RELAY = "scan_relay"

        fun intent(context: Context): Intent = Intent(context, QrActivity::class.java)
    }
}

@Composable
private fun ScannerScreen(
    title: String,
    barcodeView: DecoratedBarcodeView,
    onBack: () -> Unit,
    onLoadImage: () -> Unit,
    onPaste: () -> Unit,
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
            ScannerMenu(onLoadImage = onLoadImage, onPaste = onPaste)
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ZxingScannerView(barcodeView = barcodeView, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ScannerMenu(onLoadImage: () -> Unit, onPaste: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = PolliColors.White85)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.load_qr_code_as_image)) },
                onClick = { open = false; onLoadImage() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.paste_from_clipboard)) },
                onClick = { open = false; onPaste() },
            )
        }
    }
}
