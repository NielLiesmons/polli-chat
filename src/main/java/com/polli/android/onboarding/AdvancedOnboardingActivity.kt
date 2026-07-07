package com.polli.android.onboarding

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import chat.delta.rpc.RpcException
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.b44t.messenger.DcLot
import com.google.zxing.integration.android.IntentIntegrator
import com.polli.android.BaseAppCompatComposeActivity
import com.polli.android.media.ImageEditLauncher
import com.polli.android.navigation.AppNav
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.ui.screens.AdvancedOnboardingScreen
import androidx.compose.ui.unit.dp
import com.polli.android.platform.EngineBridge
import com.polli.android.platform.LegacyLogViewActivity
import com.polli.android.platform.LegacyProgressDialog
import com.polli.android.qr.RegistrationQrActivity
import com.polli.android.platform.PlatformAvatars
import com.polli.android.transports.EditRelayActivity
import com.polli.android.transports.ProxySettingsActivity
import com.polli.android.transports.RelayListActivity
import com.polli.android.platform.PlatformDialogs
import com.polli.android.platform.PlatformLegacyUtil
import com.polli.android.platform.PlatformPrefs
import com.polli.android.platform.PlatformThread
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.DcEventCenter
import java.io.IOException
import java.security.SecureRandom
import java.util.concurrent.Executors

/** Compose advanced onboarding — provider picker, avatar, login/create. Replaces Java InstantOnboardingActivity. */
class AdvancedOnboardingActivity : BaseAppCompatComposeActivity(), DcEventCenter.DcEventDelegate {
    private val executor = Executors.newSingleThreadExecutor()
    private var progressDialog: LegacyProgressDialog? = null
    private var cancelled = false

    private var providerHost by mutableStateOf(DEFAULT_CHATMAIL_HOST)
    private var providerQrData by mutableStateOf("$DCACCOUNT:$DEFAULT_CHATMAIL_HOST")
    private var isDcLogin by mutableStateOf(false)
    private var invitationMessage by mutableStateOf<String?>(null)
    private var displayName by mutableStateOf("")
    private var accountBusy by mutableStateOf(false)
    private var rawQrData: String? = null
    private var isContactInvitation = false
    private var isGroupInvitation = false

    private var pendingAvatar by mutableStateOf<Bitmap?>(null)
    private var avatarChanged = false

    private val imageEditor = ImageEditLauncher(
        activity = this,
        onEdited = { uri -> applyAvatarUri(uri) },
    )

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageEditor.launch(uri, cropAvatar = true)
        }
    }

    private val scanProviderQr = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        val raw = data.getStringExtra(RegistrationQrActivity.QRDATA_EXTRA)
            ?: IntentIntegrator.parseActivityResult(result.resultCode, data)?.contents
        if (!raw.isNullOrBlank()) {
            setProviderFromQr(raw)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (EngineBridge.getContext(this).isConfigured == 1) {
            val uri = intent.data
            if (uri != null) {
                startActivity(
                    Intent(this, RelayListActivity::class.java).apply {
                        putExtra(RelayListActivity.EXTRA_QR_DATA, uri.toString())
                    },
                )
            }
            finish()
            return
        }
        EngineBridge.getEventCenter(this).addObserver(DcContext.DC_EVENT_CONFIGURE_PROGRESS, this)
        loadExistingAvatar()
        displayName = EngineBridge.get(this, EngineBridge.CONFIG_DISPLAY_NAME).orEmpty()
        handleIntent(intent)
        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                AdvancedOnboardingScreen(
                    displayName = displayName,
                    onDisplayNameChange = { displayName = it },
                    providerHost = providerHost,
                    isDcLogin = isDcLogin,
                    invitationMessage = invitationMessage,
                    avatarBitmap = pendingAvatar?.asImageBitmap(),
                    topInset = AppInsets.statusBarTop(),
                    bottomInset = AppInsets.navigationBarBottom() + 24.dp,
                    externalBusy = accountBusy,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onPickAvatar = { pickAvatar.launch("image/*") },
                    onShowOtherOptions = { showOtherOptionsDialog() },
                    onPrivacyPolicyClick = {
                        if (!isDcLogin) {
                            PlatformLegacyUtil.showInBrowser(this, "https://$providerHost/privacy.html")
                        }
                    },
                    onCreate = { name -> createProfile(name) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        if (EngineBridge.getContext(this).isConfigured == 0) {
            saveDraftProfile()
        }
    }

    override fun onDestroy() {
        EngineBridge.getEventCenter(this).removeObservers(this)
        executor.shutdown()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.instant_onboarding_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val proxyItem = menu.findItem(R.id.menu_proxy_settings)
        if (proxyItem != null) {
            if (TextUtils.isEmpty(EngineBridge.get(this, EngineBridge.CONFIG_PROXY_URL))) {
                proxyItem.isVisible = false
            } else {
                val proxyEnabled = EngineBridge.getInt(this, EngineBridge.CONFIG_PROXY_ENABLED) == 1
                proxyItem.setIcon(
                    if (proxyEnabled) R.drawable.ic_proxy_enabled_24 else R.drawable.ic_proxy_disabled_24,
                )
                proxyItem.isVisible = true
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.menu_proxy_settings -> {
                startActivity(Intent(this, ProxySettingsActivity::class.java))
                true
            }
            R.id.menu_view_log -> {
                startActivity(Intent(this, LegacyLogViewActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun handleEvent(event: DcEvent) {
        if (event.id == DcContext.DC_EVENT_CONFIGURE_PROGRESS) {
            val percent = event.data1Int / 10
            progressDialog?.setMessage(getString(R.string.one_moment) + " $percent%")
        }
    }

    private fun loadExistingAvatar() {
        val file = PlatformAvatars.getSelfAvatarFile(this)
        if (file.exists() && file.length() > 0) {
            pendingAvatar = BitmapFactory.decodeFile(file.absolutePath)
        }
    }

    private fun applyAvatarUri(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { stream ->
            pendingAvatar = BitmapFactory.decodeStream(stream)
            avatarChanged = true
        }
    }

    private fun saveDraftProfile() {
        if (displayName.isNotBlank()) {
            EngineBridge.set(this, EngineBridge.CONFIG_DISPLAY_NAME, displayName)
        }
        if (avatarChanged && pendingAvatar != null) {
            try {
                PlatformAvatars.setSelfAvatar(this, pendingAvatar)
                PlatformPrefs.setProfileAvatarId(this, SecureRandom().nextInt())
                avatarChanged = false
            } catch (_: IOException) {
                // Best-effort draft save on pause.
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when {
            intent.hasExtra(EXTRA_RAW_QR) -> {
                setProviderFromQr(intent.getStringExtra(EXTRA_RAW_QR).orEmpty())
            }
            intent.data != null -> {
                setProviderFromQr(intent.data.toString())
            }
        }
    }

    private fun setProviderFromQr(rawQr: String) {
        val dc = EngineBridge.getContext(this)
        when (val state = dc.checkQr(rawQr).state) {
            DcContext.DC_QR_LOGIN -> {
                isDcLogin = true
                applyAccountQr(dc.checkQr(rawQr), rawQr)
            }
            DcContext.DC_QR_ACCOUNT -> {
                isDcLogin = false
                applyAccountQr(dc.checkQr(rawQr), rawQr)
            }
            DcContext.DC_QR_ASK_VERIFYCONTACT -> {
                val parsed = dc.checkQr(rawQr)
                isContactInvitation = true
                rawQrData = rawQr
                val name = dc.getContact(parsed.id).displayName
                invitationMessage = getString(R.string.instant_onboarding_contact_info, name)
            }
            DcContext.DC_QR_ASK_VERIFYGROUP -> {
                val parsed = dc.checkQr(rawQr)
                isGroupInvitation = true
                rawQrData = rawQr
                invitationMessage = getString(R.string.instant_onboarding_group_info, parsed.text1)
            }
            else -> {
                AlertDialog.Builder(this)
                    .setMessage(R.string.qraccount_qr_code_cannot_be_used)
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }
        }
    }

    private fun applyAccountQr(parsed: DcLot, rawQr: String) {
        providerHost = parsed.text1.orEmpty()
        providerQrData = rawQr
        invitationMessage = null
        isContactInvitation = false
        isGroupInvitation = false
    }

    private fun showOtherOptionsDialog() {
        val view = layoutInflater.inflate(R.layout.signup_options_view, null)
        val dialog =
            AlertDialog.Builder(this)
                .setView(view)
                .setTitle(R.string.instant_onboarding_show_more_instances)
                .setNegativeButton(R.string.cancel, null)
                .create()
        view.findViewById<android.widget.Button>(R.id.use_other_server)?.setOnClickListener {
            PlatformLegacyUtil.showInBrowser(this, INSTANCES_URL)
            dialog.dismiss()
        }
        view.findViewById<android.widget.Button>(R.id.login_button)?.setOnClickListener {
            startActivity(Intent(this, EditRelayActivity::class.java))
            dialog.dismiss()
        }
        view.findViewById<android.widget.Button>(R.id.scan_qr_button)?.setOnClickListener {
            scanProviderQr.launch(Intent(this, RegistrationQrActivity::class.java))
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun createProfile(name: String) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, R.string.please_enter_name, Toast.LENGTH_LONG).show()
            return
        }
        displayName = name
        accountBusy = true
        executor.execute {
            EngineBridge.set(this, EngineBridge.CONFIG_DISPLAY_NAME, name)
            var ok = true
            if (avatarChanged && pendingAvatar != null) {
                try {
                    PlatformAvatars.setSelfAvatar(this, pendingAvatar)
                    PlatformPrefs.setProfileAvatarId(this, SecureRandom().nextInt())
                } catch (_: IOException) {
                    ok = false
                }
            }
            val success = ok
            PlatformThread.runOnMain {
                if (success) {
                    startQrAccountCreation(providerQrData)
                } else {
                    accountBusy = false
                    Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startQrAccountCreation(qrCode: String) {
        progressDialog?.dismiss()
        cancelled = false
        progressDialog =
            PlatformDialogs.createProgressDialog(this).apply {
                setMessage(getString(R.string.one_moment))
                setCanceledOnTouchOutside(false)
                setCancelable(false)
                setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel)) { _, _ ->
                    cancelled = true
                    EngineBridge.getContext(this@AdvancedOnboardingActivity).stopOngoingProcess()
                }
                show()
            }
        EngineBridge.captureNextError(this)
        Thread {
            val dc = EngineBridge.getContext(this)
            try {
                EngineBridge.getRpc(this).addTransportFromQr(dc.accountId, qrCode)
                EngineBridge.endCaptureNextError(this)
                PlatformThread.runOnMain {
                    progressDialog?.dismiss()
                    val home =
                        AppNav.homeIntentFromWelcome(
                            this,
                            if (isContactInvitation || isGroupInvitation) rawQrData else null,
                        )
                    startActivity(home)
                    finishAffinity()
                }
            } catch (e: RpcException) {
                EngineBridge.endCaptureNextError(this)
                if (!cancelled) {
                    PlatformThread.runOnMain {
                        progressDialog?.dismiss()
                        accountBusy = false
                        OnboardingErrors.maybeShowConfigurationError(this, e.message)
                    }
                }
            }
        }.start()
    }

    companion object {
        private const val DCACCOUNT = "dcaccount"
        private const val INSTANCES_URL = "https://chatmail.at/relays"
        private const val DEFAULT_CHATMAIL_HOST = "nine.testrun.org"
        const val EXTRA_RAW_QR = "advanced_onboarding_raw_qr"

        fun intent(context: Context): Intent =
            Intent(context, AdvancedOnboardingActivity::class.java)

        fun intentWithQr(context: Context, rawQr: String): Intent =
            Intent(context, AdvancedOnboardingActivity::class.java).apply {
                putExtra(EXTRA_RAW_QR, rawQr)
            }
    }
}
