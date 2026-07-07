package com.polli.android.qr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.b44t.messenger.DcChat
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import com.polli.android.BaseComposeActivity
import com.polli.android.platform.EngineBridge
import com.polli.android.platform.PlatformClipboard
import com.polli.android.platform.PlatformLegacyUtil
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import com.polli.android.R
import com.polli.android.connect.DcEventCenter

/** Compose "show my invite QR" screen — replaces legacy Java QrShowActivity + QrShowFragment. */
class QrShowActivity : BaseComposeActivity(), DcEventCenter.DcEventDelegate {

    private var chatId = 0
    private var numJoiners = 0
    private val dc: DcContext get() = EngineBridge.getContext(this)

    private var title by mutableStateOf("")
    private var subtitle by mutableStateOf("")
    private var qrSvg by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        chatId = intent.getIntExtra(CHAT_ID, 0)

        if (chatId != 0) {
            title = dc.getChat(chatId).name
            subtitle = getString(R.string.qrshow_join_group_title)
        } else {
            var selfName = EngineBridge.get(this, EngineBridge.CONFIG_DISPLAY_NAME).orEmpty()
            if (selfName.isEmpty()) {
                selfName = EngineBridge.get(this, EngineBridge.CONFIG_CONFIGURED_ADDRESS) ?: "unknown"
            }
            title = selfName
            subtitle = getString(R.string.qrshow_join_contact_title)
        }

        qrSvg = runCatching { QrSvg.fixSvg(dc.getSecurejoinQrSvg(chatId)) }.getOrNull()
        if (qrSvg == null) {
            finish()
            return
        }

        EngineBridge.getEventCenter(this)
            .addObserver(DcContext.DC_EVENT_SECUREJOIN_INVITER_PROGRESS, this)

        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                QrShowContent(
                    title = title,
                    subtitle = subtitle,
                    svg = qrSvg,
                    onBack = { finish() },
                    onShare = { showInviteLinkDialog() },
                    onWithdraw = { withdrawQr() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!PlatformLegacyUtil.isNetworkConnected(this)) {
            Toast.makeText(this, R.string.qrshow_join_contact_no_connection_toast, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        EngineBridge.getEventCenter(this).removeObservers(this)
        super.onDestroy()
    }

    private fun inviteUrl(): String = dc.getSecurejoinQr(chatId)

    private fun showInviteLinkDialog() {
        val url = inviteUrl()
        AlertDialog.Builder(this)
            .setMessage(url)
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.menu_copy_to_clipboard) { _, _ ->
                PlatformClipboard.copyText(this, url)
                Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton(R.string.menu_share) { _, _ ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.chat_share_with_title)))
            }
            .create()
            .show()
    }

    private fun withdrawQr() {
        val message = when {
            chatId == 0 -> getString(R.string.withdraw_verifycontact_explain)
            else -> {
                val chat = dc.getChat(chatId)
                if (chat.type == DcChat.DC_CHAT_TYPE_GROUP) {
                    getString(R.string.withdraw_verifygroup_explain, chat.name)
                } else {
                    getString(R.string.withdraw_joinbroadcast_explain, chat.name)
                }
            }
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.withdraw_qr_code)
            .setMessage(message)
            .setPositiveButton(R.string.reset) { _, _ ->
                dc.setConfigFromQr(inviteUrl())
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
        PlatformLegacyUtil.redPositiveButton(dialog)
    }

    override fun handleEvent(event: DcEvent) {
        if (event.id != DcContext.DC_EVENT_SECUREJOIN_INVITER_PROGRESS) return
        val contactId = event.data1Int
        val progress = event.data2Int
        val msg = when (progress) {
            300 -> {
                numJoiners++
                String.format(getString(R.string.qrshow_x_joining), dc.getContact(contactId).displayName)
            }
            600 -> String.format(getString(R.string.qrshow_x_verified), dc.getContact(contactId).displayName)
            800 -> String.format(getString(R.string.qrshow_x_has_joined_group), dc.getContact(contactId).displayName)
            else -> null
        }
        if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        if (progress == 1000) {
            numJoiners--
            if (numJoiners <= 0) finish()
        }
    }

    companion object {
        const val CHAT_ID = "chat_id"

        fun intent(context: Context): Intent = Intent(context, QrShowActivity::class.java)
    }
}

@Composable
private fun QrShowContent(
    title: String,
    subtitle: String,
    svg: String?,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onWithdraw: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .padding(top = AppInsets.statusBarTop() + 8.dp)
            .padding(bottom = AppInsets.navigationBarBottom() + 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = PolliColors.White85, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, color = PolliColors.White33, style = MaterialTheme.typography.bodySmall)
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null, tint = PolliColors.White85)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.withdraw_qr_code), color = PolliColors.Rouge) },
                        onClick = { menuOpen = false; onWithdraw() },
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            if (svg != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PolliColors.White)
                        .padding(16.dp),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        factory = { ctx -> SVGImageView(ctx) },
                        update = { view -> runCatching { view.setSVG(SVG.getFromString(svg)) } },
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onShare) {
                Text(stringResource(R.string.menu_share), color = PolliColors.White)
            }
        }
    }
}
