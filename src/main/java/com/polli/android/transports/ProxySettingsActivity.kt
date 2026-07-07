package com.polli.android.transports

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import com.polli.android.BaseComposeActivity
import com.polli.android.platform.EngineBridge
import com.polli.android.platform.PlatformLegacyUtil
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import com.polli.android.ui.ShellDivider
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.DcEventCenter

/** Compose proxy settings — replaces legacy Java ProxySettingsActivity. */
class ProxySettingsActivity : BaseComposeActivity(), DcEventCenter.DcEventDelegate {

    private val proxies: SnapshotStateList<String> = mutableStateListOf()
    private var selectedProxy by mutableStateOf<String?>(null)
    private var proxyEnabled by mutableStateOf(false)
    private var connectivityLabel by mutableStateOf<String?>(null)

    private val dc: DcContext get() = EngineBridge.getContext(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EngineBridge.getEventCenter(this).addObserver(DcContext.DC_EVENT_CONNECTIVITY_CHANGED, this)

        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                ProxyContent()
            }
        }
        reload()
        handleOpenProxyUrl(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenProxyUrl(intent)
    }

    override fun onDestroy() {
        EngineBridge.getEventCenter(this).removeObservers(this)
        super.onDestroy()
    }

    override fun handleEvent(event: DcEvent) {
        if (event.id == DcContext.DC_EVENT_CONNECTIVITY_CHANGED) {
            refreshConnectivity()
        }
    }

    private fun reload() {
        val urls = EngineBridge.get(this, EngineBridge.CONFIG_PROXY_URL).orEmpty()
        proxies.clear()
        if (urls.isNotEmpty()) proxies.addAll(urls.split("\n").filter { it.isNotEmpty() })
        selectedProxy = proxies.firstOrNull()
        proxyEnabled = EngineBridge.getInt(this, EngineBridge.CONFIG_PROXY_ENABLED) == 1
        refreshConnectivity()
    }

    private fun refreshConnectivity() {
        connectivityLabel =
            if (!proxyEnabled || dc.isConfigured() != 1) {
                null
            } else {
                val connectivity = dc.connectivity
                when {
                    connectivity >= DcContext.DC_CONNECTIVITY_WORKING ->
                        getString(R.string.connectivity_connected)
                    connectivity >= DcContext.DC_CONNECTIVITY_CONNECTING ->
                        getString(R.string.connectivity_connecting)
                    else -> getString(R.string.connectivity_not_connected)
                }
            }
    }

    private fun onToggle(checked: Boolean) {
        if (checked && proxies.isEmpty()) {
            showAddProxyDialog(revertSwitchOnCancel = true)
            return
        }
        EngineBridge.set(this, EngineBridge.CONFIG_PROXY_ENABLED, if (checked) "1" else "0")
        dc.restartIo()
        proxyEnabled = checked
        refreshConnectivity()
    }

    private fun onProxyClick(proxyUrl: String) {
        if (dc.setConfigFromQr(proxyUrl)) {
            dc.restartIo()
            selectedProxy = proxyUrl
            proxyEnabled = EngineBridge.getInt(this, EngineBridge.CONFIG_PROXY_ENABLED) == 1
            refreshConnectivity()
        } else {
            Toast.makeText(this, R.string.proxy_invalid, Toast.LENGTH_LONG).show()
        }
    }

    private fun onProxyShare(proxyUrl: String) {
        val svgView = SVGImageView(this)
        runCatching { svgView.setSVG(SVG.getFromString(dc.createQrSvg(proxyUrl))) }
        val padding = (resources.displayMetrics.density * 16).toInt()
        svgView.setPadding(padding, padding, padding, padding)
        AlertDialog.Builder(this)
            .setView(svgView)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.proxy_share_link) { _, _ ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, proxyUrl)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.chat_share_with_title)))
            }
            .show()
    }

    private fun onProxyDelete(proxyUrl: String) {
        val host = dc.checkQr(proxyUrl).text1
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.proxy_delete)
            .setMessage(getString(R.string.proxy_delete_explain, host))
            .setPositiveButton(R.string.delete) { _, _ -> deleteProxy(proxyUrl) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        PlatformLegacyUtil.redPositiveButton(dialog)
    }

    private fun deleteProxy(proxyUrl: String) {
        val remaining = EngineBridge.get(this, EngineBridge.CONFIG_PROXY_URL).orEmpty()
            .split("\n").filter { it.isNotEmpty() && it != proxyUrl }
        if (remaining.isEmpty()) {
            EngineBridge.set(this, EngineBridge.CONFIG_PROXY_ENABLED, "0")
            proxyEnabled = false
        }
        EngineBridge.set(this, EngineBridge.CONFIG_PROXY_URL, remaining.joinToString("\n"))
        dc.restartIo()
        reload()
    }

    private fun showAddProxyDialog(revertSwitchOnCancel: Boolean) {
        val input = EditText(this).apply { setHint(R.string.proxy_add_url_hint) }
        val pad = (resources.displayMetrics.density * 20).toInt()
        val container = android.widget.FrameLayout(this).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.proxy_add)
            .setMessage(R.string.proxy_add_explain)
            .setView(container)
            .setPositiveButton(R.string.proxy_use_proxy) { _, _ ->
                val newProxy = input.text.toString().trim()
                if (dc.checkQr(newProxy).state == DcContext.DC_QR_PROXY) {
                    dc.setConfigFromQr(newProxy)
                    dc.restartIo()
                    reload()
                } else {
                    Toast.makeText(this, R.string.proxy_invalid, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                if (revertSwitchOnCancel && proxies.isEmpty()) proxyEnabled = false
            }
            .setCancelable(false)
            .show()
    }

    private fun handleOpenProxyUrl(intent: Intent?) {
        if (intent == null || Intent.ACTION_VIEW != intent.action) return
        val uri = intent.data ?: return
        val parsed = dc.checkQr(uri.toString())
        if (parsed.state == DcContext.DC_QR_PROXY) {
            AlertDialog.Builder(this)
                .setTitle(R.string.proxy_use_proxy)
                .setMessage(getString(R.string.proxy_use_proxy_confirm, parsed.text1))
                .setPositiveButton(R.string.proxy_use_proxy) { _, _ ->
                    dc.setConfigFromQr(uri.toString())
                    dc.restartIo()
                    reload()
                }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false)
                .show()
        } else {
            Toast.makeText(this, R.string.proxy_invalid, Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    private fun ProxyContent() {
        Box(modifier = Modifier.fillMaxSize().background(PolliColors.Black)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .padding(top = AppInsets.statusBarTop() + 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundBackButton(onClick = { finish() })
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.proxy_settings),
                        color = PolliColors.White85,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = proxyEnabled,
                        onCheckedChange = { onToggle(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PolliColors.White),
                    )
                }
                ShellDivider()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(proxies, key = { it }) { proxyUrl ->
                        ProxyRow(proxyUrl)
                        ShellDivider()
                    }
                }
            }
        }
    }

    @Composable
    private fun ProxyRow(proxyUrl: String) {
        val parsed = remember(proxyUrl) { dc.checkQr(proxyUrl) }
        val isProxy = parsed.state == DcContext.DC_QR_PROXY
        val host = if (isProxy) parsed.text1 else proxyUrl
        val protocol = if (isProxy) proxyUrl.split(":", limit = 2)[0] else stringResource(R.string.unknown)
        val isSelected = proxyUrl == selectedProxy
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { onProxyClick(proxyUrl) }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(host, color = PolliColors.White85, style = MaterialTheme.typography.bodyLarge)
                Text(protocol, color = PolliColors.White33, style = MaterialTheme.typography.bodySmall)
                if (isSelected && connectivityLabel != null) {
                    Text(connectivityLabel!!, color = PolliColors.White66, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = PolliColors.White85,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = { onProxyShare(proxyUrl) }) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.proxy_share_link), tint = PolliColors.White66)
            }
            IconButton(onClick = { onProxyDelete(proxyUrl) }) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.proxy_delete), tint = PolliColors.White66)
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, ProxySettingsActivity::class.java)
    }
}
