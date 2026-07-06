package com.polli.android.share

import android.Manifest
import android.content.Intent
import android.net.MailTo
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.lifecycleScope
import com.b44t.messenger.DcContext
import com.polli.android.HomeRelayingActivity
import com.polli.android.navigation.AppNav
import com.polli.android.platform.EngineBridge
import com.polli.android.platform.PlatformMedia
import com.polli.android.platform.PlatformShare
import com.polli.domain.navigation.ChatIntentExtras
import kotlinx.coroutines.launch
import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.permissions.Permissions
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme

/**
 * OS share target — resolves inbound URIs and routes to [HomeRelayingActivity] or a direct chat.
 */
class PolliShareActivity : PassphraseRequiredActionBarActivity() {
    private lateinit var dcContext: DcContext
    private val resolvedExtras = ArrayList<Uri>()

    override fun onPreCreate() {
        dynamicTheme = DynamicNoActionBarTheme()
        super.onPreCreate()
    }

    override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
        dcContext = EngineBridge.getContext(this)
        setContentView(R.layout.share_activity)
        initializeToolbar()
        initializeMedia()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resolvedExtras.clear()
        initializeMedia()
    }

    private fun initializeToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initializeMedia() {
        normalizeMailtoIntent()
        val streamExtras = collectStreamExtras()
        if (needsFilePermission(streamExtras)) {
            if (Permissions.hasAll(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                resolveUris(streamExtras)
            } else {
                Permissions.with(this)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                    .alwaysGrantOnSdk33()
                    .ifNecessary()
                    .withPermanentDenialDialog(getString(R.string.perm_explain_access_to_storage_denied))
                    .onAllGranted { resolveUris(streamExtras) }
                    .onAnyDenied { abortShare() }
                    .execute()
            }
        } else {
            resolveUris(streamExtras)
        }
    }

    private fun normalizeMailtoIntent() {
        val data = intent.data ?: return
        if (!"mailto".equals(data.scheme, ignoreCase = true)) return
        val mailto = MailTo.parse(data.toString())
        if (intent.getStringArrayExtra(Intent.EXTRA_EMAIL) == null) {
            val recipients = mailto.to?.trim()?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toTypedArray()
            if (!recipients.isNullOrEmpty()) {
                intent.putExtra(Intent.EXTRA_EMAIL, recipients)
            }
        }
        if (intent.getStringExtra(Intent.EXTRA_TEXT).isNullOrEmpty()) {
            val subject = mailto.subject.orEmpty()
            val body = mailto.body.orEmpty()
            val text =
                when {
                    subject.isNotBlank() && body.isNotBlank() -> "$subject\n$body"
                    subject.isNotBlank() -> subject
                    else -> body
                }
            if (text.isNotBlank()) intent.putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    private fun collectStreamExtras(): List<Uri> {
        val extras = ArrayList<Uri>()
        if (Intent.ACTION_SEND == intent.action) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { extras.add(it) }
        } else {
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { extras.addAll(it) }
        }
        return extras
    }

    private fun needsFilePermission(uris: List<Uri>): Boolean =
        uris.any { it.scheme == "file" }

    private fun resolveUris(streamExtras: List<Uri>) {
        if (streamExtras.isEmpty()) {
            handleResolvedMedia(intent)
            return
        }
        lifecycleScope.launch {
            for (uri in streamExtras) {
                if (uri == null) continue
                ShareMediaResolver.resolve(this@PolliShareActivity, uri)?.let { resolvedExtras.add(it) }
            }
            handleResolvedMedia(intent)
        }
    }

    private fun abortShare() {
        Toast.makeText(this, R.string.share_abort, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleResolvedMedia(intent: Intent) {
        var accId = intent.getIntExtra(EXTRA_ACC_ID, -1)
        var chatId = intent.getIntExtra(EXTRA_CHAT_ID, -1)

        val shortcutId = intent.getStringExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID)
        if ((chatId == -1 || accId == -1) && shortcutId != null && shortcutId.startsWith("chat-")) {
            val args = shortcutId.split("-")
            if (args.size == 3) {
                accId = args[1].toIntOrNull() ?: accId
                chatId = args[2].toIntOrNull() ?: chatId
            }
        }

        val extraEmail = intent.getStringArrayExtra(Intent.EXTRA_EMAIL)
        if (chatId == -1 && !extraEmail.isNullOrEmpty()) {
            val addr = extraEmail[0]
            var contactId = dcContext.lookupContactIdByAddr(addr)
            if (contactId == 0) contactId = dcContext.createContact(null, addr)
            chatId = dcContext.createChatByContactId(contactId)
            accId = dcContext.accountId
        }

        val composeIntent =
            if (accId != -1 && chatId > 0) {
                buildShareIntent(AppNav.chatActivityClass()).apply {
                    putExtra(ChatIntentExtras.CHAT_ID, chatId)
                    putExtra(ChatIntentExtras.ACCOUNT_ID, accId)
                }
            } else {
                buildShareIntent(HomeRelayingActivity::class.java).also {
                    PlatformShare.setIsFromWebxdc(it, PlatformShare.isFromWebxdc(this))
                }
            }
        PlatformShare.setSharedUris(composeIntent, resolvedExtras)
        if (accId != -1 && chatId > 0) {
            startActivity(composeIntent)
        } else {
            HomeRelayingActivity.start(this, composeIntent)
        }
        finish()
    }

    private fun buildShareIntent(target: Class<*>): Intent {
        val out = Intent(this, target)
        PlatformShare.getSharedTitle(this)?.let { PlatformShare.setSharedTitle(out, it) }
        var text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (text == null) {
            text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        }
        if (!text.isNullOrEmpty()) PlatformShare.setSharedText(out, text)
        if (resolvedExtras.isNotEmpty()) {
            val data = resolvedExtras[0]
            val mimeType = PlatformMedia.mimeType(this, data) ?: intent.type
            out.setDataAndType(data, mimeType)
        }
        return out
    }

    companion object {
        const val EXTRA_ACC_ID: String = "acc_id"
        const val EXTRA_CHAT_ID: String = "chat_id"
    }
}
