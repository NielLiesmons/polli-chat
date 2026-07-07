package com.polli.android.qr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.b44t.messenger.DcBackupProvider
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import com.polli.android.BaseComposeActivity
import com.polli.android.navigation.AppNav
import com.polli.android.platform.EngineBridge
import com.polli.android.debug.LogViewActivity
import com.polli.android.platform.PlatformClipboard
import com.polli.android.platform.PlatformLegacyUtil
import com.polli.android.profiles.ProfilesActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.DcEventCenter
import org.thoughtcrime.securesms.service.GenericForegroundService
import org.thoughtcrime.securesms.service.NotificationController
import java.util.Locale

/** Compose account backup transfer between two devices — replaces the Java activity + fragments. */
class BackupTransferActivity : BaseComposeActivity(), DcEventCenter.DcEventDelegate {

    private enum class TransferState { UNKNOWN, ERROR, SUCCESS }

    private var transferMode = RECEIVER_SCAN_QR
    private var transferState = TransferState.UNKNOWN
    private var warnAboutCopiedQrCodeOnAbort = false
    private var isFinishingTransfer = false
    private var notificationControllerClosed = false

    private lateinit var notificationController: NotificationController
    private val dc: DcContext get() = EngineBridge.getContext(this)

    private var dcBackupProvider: DcBackupProvider? = null
    private var prepareThread: Thread? = null
    private var waitThread: Thread? = null
    private var receiveThread: Thread? = null

    private var statusText by mutableStateOf("")
    private var showProgress by mutableStateOf(true)
    private var indeterminate by mutableStateOf(true)
    private var progressMax by mutableIntStateOf(0)
    private var progressValue by mutableIntStateOf(0)
    private var qrSvg by mutableStateOf<String?>(null)
    private var showTopText by mutableStateOf(false)
    private var showSameNetworkHint by mutableStateOf(false)
    private var sameNetworkHint by mutableStateOf("")
    private var canCopy by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        transferMode = intent.getIntExtra(TRANSFER_MODE, INVALID)
        if (transferMode == INVALID) throw RuntimeException("bad transfer mode")

        EngineBridge.getAccounts(this).stopIo()

        val title = getString(
            if (transferMode == RECEIVER_SCAN_QR) R.string.multidevice_receiver_title else R.string.multidevice_title,
        )
        notificationController = GenericForegroundService.startForegroundTask(this, title)

        sameNetworkHint = getString(R.string.multidevice_same_network_hint)
        EngineBridge.getEventCenter(this).addObserver(DcContext.DC_EVENT_IMEX_PROGRESS, this)

        when (transferMode) {
            SENDER_SHOW_QR -> startSender()
            RECEIVER_SCAN_QR -> startReceiver()
        }
        appendSsidHint()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finishOrAskToFinish()
        })

        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                BackupTransferScreen(
                    title = title,
                    statusText = statusText,
                    showProgress = showProgress,
                    indeterminate = indeterminate,
                    progressMax = progressMax,
                    progressValue = progressValue,
                    qrSvg = qrSvg,
                    showTopText = showTopText,
                    showSameNetworkHint = showSameNetworkHint,
                    sameNetworkHint = sameNetworkHint,
                    canCopy = canCopy,
                    onBack = { finishOrAskToFinish() },
                    onCopy = { copyQr() },
                    onTroubleshooting = { PlatformLegacyUtil.openHelp(this, "#multiclient") },
                    onViewLog = { startActivity(Intent(this, LogViewActivity::class.java)) },
                )
            }
        }
    }

    private fun startSender() {
        statusText = getString(R.string.preparing_account)
        indeterminate = true
        showProgress = true
        val thread = Thread {
            Log.i(TAG, "##### newBackupProvider()")
            val provider = dc.newBackupProvider()
            dcBackupProvider = provider
            Log.i(TAG, "##### newBackupProvider() returned")
            runOnUiThread {
                if (isFinishing || isFinishingTransfer) return@runOnUiThread
                showProgress = false
                if (!provider.isOk) {
                    setTransferError("Cannot create backup provider")
                    return@runOnUiThread
                }
                qrSvg = runCatching { QrSvg.fixSvg(provider.qrSvg) }.getOrNull()
                showTopText = true
                canCopy = true
                val wait = Thread {
                    Log.i(TAG, "##### waitForReceiver() with qr: " + provider.qr)
                    provider.waitForReceiver()
                    Log.i(TAG, "##### done waiting")
                }
                waitThread = wait
                wait.start()
            }
        }
        prepareThread = thread
        thread.start()
    }

    private fun startReceiver() {
        statusText = getString(R.string.connectivity_connecting)
        indeterminate = true
        showProgress = true
        showSameNetworkHint = true
        val qrCode = intent.getStringExtra(QR_CODE)
        receiveThread = Thread {
            Log.i(TAG, "##### receiveBackup()")
            val res = dc.receiveBackup(qrCode)
            Log.i(TAG, "##### receiveBackup() done with result: $res")
        }.also { it.start() }
    }

    private fun appendSsidHint() {
        Thread {
            val ssid = WifiSsid.current(this)
            if (ssid != null) runOnUiThread { sameNetworkHint = "$sameNetworkHint ($ssid)" }
        }.start()
    }

    private fun copyQr() {
        val provider = dcBackupProvider ?: return
        PlatformClipboard.copyText(this, provider.qr)
        Toast.makeText(this, R.string.done, Toast.LENGTH_SHORT).show()
        warnAboutCopiedQrCodeOnAbort = true
    }

    private fun setTransferError(errorContext: String) {
        if (transferState == TransferState.ERROR) return
        transferState = TransferState.ERROR
        val lastError = dc.lastError.ifEmpty { "<last error not set>" }
        val error = if (errorContext.isEmpty()) lastError else "$errorContext: $lastError"
        AlertDialog.Builder(this)
            .setMessage(error)
            .setPositiveButton(android.R.string.ok, null)
            .setCancelable(false)
            .show()
    }

    private fun finishOrAskToFinish() {
        when (transferState) {
            TransferState.ERROR, TransferState.SUCCESS -> doFinish()
            TransferState.UNKNOWN -> {
                var msg = getString(R.string.multidevice_abort)
                if (warnAboutCopiedQrCodeOnAbort) {
                    msg += "\n\n" + getString(R.string.multidevice_abort_will_invalidate_copied_qr)
                }
                AlertDialog.Builder(this)
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.ok) { _, _ -> doFinish() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun doFinish() {
        notificationController.close()
        notificationControllerClosed = true
        if (transferMode == RECEIVER_SCAN_QR && transferState == TransferState.SUCCESS) {
            startActivity(AppNav.homeIntent(applicationContext))
        } else if (transferMode == SENDER_SHOW_QR) {
            startActivity(AppNav.homeIntent(applicationContext))
            startActivity(ProfilesActivity.intent(applicationContext))
            overridePendingTransition(0, 0)
        }
        finish()
    }

    override fun onDestroy() {
        isFinishingTransfer = true
        EngineBridge.getEventCenter(this).removeObservers(this)
        runCatching { dc.stopOngoingProcess() }
        runCatching { prepareThread?.join() }
        runCatching { waitThread?.join() }
        dcBackupProvider?.unref()
        if (!notificationControllerClosed) notificationController.close()
        EngineBridge.getAccounts(this).startIo()
        super.onDestroy()
    }

    override fun handleEvent(event: DcEvent) {
        if (event.id != DcContext.DC_EVENT_IMEX_PROGRESS) return
        if (transferMode == SENDER_SHOW_QR) handleSenderProgress(event) else handleReceiverProgress(event)
    }

    private fun handleSenderProgress(event: DcEvent) {
        if (isFinishingTransfer) return
        val permille = event.data1Int
        var percent = 0
        var percentMax = 0
        var hideQrCode = false
        var text = ""
        Log.i(TAG, "DC_EVENT_IMEX_PROGRESS, $permille")
        when {
            permille == 0 -> { setTransferError("Sending Error"); hideQrCode = true }
            permille < 1000 -> { percent = permille / 10; percentMax = 100; text = getString(R.string.transferring); hideQrCode = true }
            permille == 1000 -> {
                text = getString(R.string.done) + " \uD83D\uDE00"
                transferState = TransferState.SUCCESS
                showProgress = false
                hideQrCode = true
            }
        }
        statusText = text
        notificationController.setProgress(percentMax.toLong(), percent.toLong(), text)
        applyProgress(percentMax, percent)
        if (hideQrCode && qrSvg != null) {
            qrSvg = null
            showTopText = false
            canCopy = false
            showProgress = permille != 1000
        }
    }

    private fun handleReceiverProgress(event: DcEvent) {
        val permille = event.data1Int
        var percent = 0
        var percentMax = 0
        var hideHint = false
        var text = ""
        Log.i(TAG, "DC_EVENT_IMEX_PROGRESS, $permille")
        when {
            permille == 0 -> {
                PlatformLegacyUtil.maybeShowMigrationError(this)
                setTransferError("Receiving Error")
            }
            permille < 1000 -> {
                percent = permille / 10
                percentMax = 100
                val formatted = if (percent > 0) String.format(Locale.getDefault(), " %d%%", percent) else ""
                text = getString(R.string.transferring) + formatted
                hideHint = true
            }
            permille == 1000 -> {
                transferState = TransferState.SUCCESS
                doFinish()
                return
            }
        }
        statusText = text
        notificationController.setProgress(percentMax.toLong(), percent.toLong(), text)
        applyProgress(percentMax, percent)
        if (hideHint) showSameNetworkHint = false
    }

    private fun applyProgress(percentMax: Int, percent: Int) {
        if (percentMax == 0) {
            indeterminate = true
        } else {
            indeterminate = false
            progressMax = percentMax
            progressValue = percent
        }
    }

    companion object {
        private const val TAG = "BackupTransferActivity"

        const val INVALID = 0
        const val SENDER_SHOW_QR = 1
        const val RECEIVER_SCAN_QR = 2

        const val TRANSFER_MODE = "transfer_mode"
        const val QR_CODE = "qr_code"

        fun senderIntent(context: Context): Intent =
            Intent(context, BackupTransferActivity::class.java).putExtra(TRANSFER_MODE, SENDER_SHOW_QR)

        fun receiverIntent(context: Context, qrCode: String): Intent =
            Intent(context, BackupTransferActivity::class.java)
                .putExtra(TRANSFER_MODE, RECEIVER_SCAN_QR)
                .putExtra(QR_CODE, qrCode)
    }
}

@Composable
private fun BackupTransferScreen(
    title: String,
    statusText: String,
    showProgress: Boolean,
    indeterminate: Boolean,
    progressMax: Int,
    progressValue: Int,
    qrSvg: String?,
    showTopText: Boolean,
    showSameNetworkHint: Boolean,
    sameNetworkHint: String,
    canCopy: Boolean,
    onBack: () -> Unit,
    onCopy: () -> Unit,
    onTroubleshooting: () -> Unit,
    onViewLog: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .padding(top = AppInsets.statusBarTop() + 8.dp)
            .padding(bottom = AppInsets.navigationBarBottom() + 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = PolliColors.White85)
            }
            Text(
                title,
                color = PolliColors.White85,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                maxLines = 1,
            )
            BackupMenu(canCopy = canCopy, onCopy = onCopy, onTroubleshooting = onTroubleshooting, onViewLog = onViewLog)
        }

        if (statusText.isNotEmpty()) {
            Text(
                statusText,
                color = PolliColors.White85,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 24.dp, end = 24.dp),
            )
        }

        if (showProgress) {
            Spacer(modifier = Modifier.height(16.dp))
            if (indeterminate) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            } else {
                LinearProgressIndicator(
                    progress = { if (progressMax > 0) progressValue.toFloat() / progressMax else 0f },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
        }

        if (showTopText) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                HintLine("\u2780", sameNetworkHint)
                HintLine("\u2781", stringResource(R.string.multidevice_install_dc_on_other_device))
                HintLine("\u2782", stringResource(R.string.multidevice_tap_scan_on_other_device))
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            if (qrSvg != null) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx -> SVGImageView(ctx) },
                    update = { view -> runCatching { view.setSVG(SVG.getFromString(qrSvg)) } },
                )
            } else if (showSameNetworkHint && sameNetworkHint.isNotEmpty()) {
                Text(
                    sameNetworkHint,
                    color = PolliColors.White66,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun HintLine(marker: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Start) {
        Text(marker, color = PolliColors.White66, modifier = Modifier.width(28.dp))
        Text(text, color = PolliColors.White66, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BackupMenu(canCopy: Boolean, onCopy: () -> Unit, onTroubleshooting: () -> Unit, onViewLog: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = PolliColors.White85)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.troubleshooting)) },
                onClick = { open = false; onTroubleshooting() },
            )
            if (canCopy) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_copy_to_clipboard)) },
                    onClick = { open = false; onCopy() },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pref_view_log)) },
                onClick = { open = false; onViewLog() },
            )
        }
    }
}
