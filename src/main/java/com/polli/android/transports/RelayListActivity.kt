@file:OptIn(ExperimentalFoundationApi::class)

package com.polli.android.transports

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException
import chat.delta.rpc.types.TransportListEntry
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.google.zxing.integration.android.IntentIntegrator
import com.polli.android.BaseComposeActivity
import com.polli.android.platform.EngineBridge
import com.polli.android.platform.PlatformLegacyUtil
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import com.polli.android.ui.ShellDivider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.DcEventCenter
import org.thoughtcrime.securesms.qr.QrActivity
import org.thoughtcrime.securesms.qr.QrCodeHandler
import org.thoughtcrime.securesms.util.ScreenLockUtil

/** Compose relay (transport) list — replaces the legacy Java RelayListActivity. */
class RelayListActivity : BaseComposeActivity(), DcEventCenter.DcEventDelegate {

    private val relays: SnapshotStateList<TransportListEntry> = mutableStateListOf()
    private var mainRelayAddr by mutableStateOf("")

    private lateinit var rpc: Rpc
    private var accId: Int = 0
    private var qrData: String? = null

    private lateinit var qrScannerLauncher: ActivityResultLauncher<Intent>
    private lateinit var screenLockLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rpc = EngineBridge.getRpc(this)
        accId = EngineBridge.getContext(this).accountId

        qrScannerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val scan = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
                    QrCodeHandler(this).handleOnlyAddRelayQr(scan?.contents, null)
                }
            }
        screenLockLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode != Activity.RESULT_OK) {
                    finish()
                    return@registerForActivityResult
                }
                qrData?.let {
                    QrCodeHandler(this).handleOnlyAddRelayQr(it, null)
                    qrData = null
                }
            }

        qrData = intent.getStringExtra(EXTRA_QR_DATA)
        if (qrData != null) {
            val locked =
                ScreenLockUtil.applyScreenLock(
                    this,
                    getString(R.string.add_transport),
                    getString(R.string.enter_system_secret_to_continue),
                    screenLockLauncher,
                )
            if (!locked) {
                QrCodeHandler(this).handleOnlyAddRelayQr(qrData, null)
            }
        }

        val eventCenter = EngineBridge.getEventCenter(this)
        eventCenter.addObserver(DcContext.DC_EVENT_CONFIGURE_PROGRESS, this)
        eventCenter.addObserver(DcContext.DC_EVENT_TRANSPORTS_MODIFIED, this)

        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                RelayListContent(
                    relays = relays,
                    mainRelayAddr = mainRelayAddr,
                    onBack = { finish() },
                    onRelayClick = ::onRelayClick,
                    onAddRelay = ::launchQrScanner,
                    onEditRelay = { openEdit(it) },
                    onDeleteRelay = { confirmDelete(it) },
                )
            }
        }

        loadRelays()
    }

    override fun onDestroy() {
        EngineBridge.getEventCenter(this).removeObservers(this)
        super.onDestroy()
    }

    override fun handleEvent(event: DcEvent) {
        when (event.id) {
            DcContext.DC_EVENT_CONFIGURE_PROGRESS -> if (event.data1Int == 1000) loadRelays()
            DcContext.DC_EVENT_TRANSPORTS_MODIFIED -> loadRelays()
        }
    }

    private fun loadRelays() {
        lifecycleScope.launch {
            val mainAddr =
                withContext(Dispatchers.IO) {
                    try {
                        rpc.getConfig(accId, EngineBridge.CONFIG_CONFIGURED_ADDRESS).orEmpty()
                    } catch (e: RpcException) {
                        Log.e(TAG, "getConfig() failed", e)
                        ""
                    }
                }
            val loaded =
                withContext(Dispatchers.IO) {
                    try {
                        rpc.listTransportsEx(accId)
                    } catch (e: RpcException) {
                        Log.e(TAG, "listTransportsEx() failed", e)
                        emptyList()
                    }
                }
            mainRelayAddr = mainAddr
            relays.clear()
            relays.addAll(loaded)
        }
    }

    private fun onRelayClick(relay: TransportListEntry) {
        val addr = relay.param.addr ?: return
        if (addr == mainRelayAddr) return
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    rpc.setConfig(accId, EngineBridge.CONFIG_CONFIGURED_ADDRESS, addr)
                } catch (e: RpcException) {
                    Log.e(TAG, "setConfig() failed", e)
                }
            }
            loadRelays()
        }
    }

    private fun launchQrScanner() {
        val intent =
            IntentIntegrator(this)
                .setCaptureActivity(QrActivity::class.java)
                .addExtra(QrActivity.EXTRA_SCAN_RELAY, true)
                .createScanIntent()
        qrScannerLauncher.launch(intent)
    }

    private fun openEdit(relay: TransportListEntry) {
        startActivity(
            Intent(this, EditRelayActivity::class.java)
                .putExtra(EditRelayActivity.EXTRA_ADDR, relay.param.addr),
        )
    }

    private fun confirmDelete(relay: TransportListEntry) {
        val addr = relay.param.addr ?: return
        val dialog =
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.remove_transport)
                .setMessage(getString(R.string.confirm_remove_or_hide_transport_x, addr))
                .setPositiveButton(R.string.remove_transport) { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                rpc.deleteTransport(accId, addr)
                            } catch (e: RpcException) {
                                Log.e(TAG, "deleteTransport() failed", e)
                            }
                        }
                        loadRelays()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.hide_from_contacts) { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                rpc.setTransportUnpublished(accId, addr, true)
                            } catch (e: RpcException) {
                                Log.e(TAG, "setTransportUnpublished() failed", e)
                            }
                        }
                        loadRelays()
                    }
                }
                .show()
        PlatformLegacyUtil.redPositiveButton(dialog)
    }

    companion object {
        private const val TAG = "RelayListActivity"
        const val EXTRA_QR_DATA = "qr_data"

        fun intent(context: Context): Intent = Intent(context, RelayListActivity::class.java)
    }
}

@Composable
private fun RelayListContent(
    relays: List<TransportListEntry>,
    mainRelayAddr: String,
    onBack: () -> Unit,
    onRelayClick: (TransportListEntry) -> Unit,
    onAddRelay: () -> Unit,
    onEditRelay: (TransportListEntry) -> Unit,
    onDeleteRelay: (TransportListEntry) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(PolliColors.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = AppInsets.statusBarTop() + 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundBackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.transports),
                    color = PolliColors.White85,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            ShellDivider()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(relays, key = { it.param.addr ?: it.hashCode().toString() }) { relay ->
                    RelayRow(
                        relay = relay,
                        isMain = relay.param.addr != null && relay.param.addr == mainRelayAddr,
                        onClick = { onRelayClick(relay) },
                        onEdit = { onEditRelay(relay) },
                        onDelete = { onDeleteRelay(relay) },
                    )
                    ShellDivider()
                }
            }
        }
        FloatingActionButton(
            onClick = onAddRelay,
            containerColor = PolliColors.Gray,
            contentColor = PolliColors.White85,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = AppInsets.navigationBarBottom() + 20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_transport))
        }
    }
}

@Composable
private fun RelayRow(
    relay: TransportListEntry,
    isMain: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val addr = relay.param.addr.orEmpty()
    val parts = addr.split("@")
    val title = if (parts.size == 2) parts[1] else parts.getOrElse(0) { addr }
    val extras = buildList {
        if (parts.size == 2) add(parts[0])
        if (isMain) add(stringResource(R.string.used_for_sending))
        if (relay.isUnpublished == true) add(stringResource(R.string.hidden_from_contacts))
    }
    Box {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = PolliColors.White85, style = MaterialTheme.typography.bodyLarge)
                if (extras.isNotEmpty()) {
                    Text(
                        extras.joinToString(" · "),
                        color = PolliColors.White33,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (isMain) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = stringResource(R.string.used_for_sending),
                    tint = PolliColors.White85,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_transport)) },
                onClick = {
                    menuOpen = false
                    onEdit()
                },
            )
            if (!isMain) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.remove_transport), color = PolliColors.Rouge) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}
