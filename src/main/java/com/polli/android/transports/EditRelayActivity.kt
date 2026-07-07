package com.polli.android.transports

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException
import chat.delta.rpc.types.EnteredCertificateChecks
import chat.delta.rpc.types.EnteredLoginParam
import chat.delta.rpc.types.Socket
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.b44t.messenger.DcProvider
import com.polli.android.BaseComposeActivity
import com.polli.android.navigation.AppNav
import com.polli.android.onboarding.OnboardingErrors
import com.polli.android.platform.EngineBridge
import com.polli.android.debug.LogViewActivity
import com.polli.android.platform.LegacyProgressDialog
import com.polli.android.platform.PlatformDialogs
import com.polli.android.platform.PlatformLegacyUtil
import com.polli.android.platform.PlatformThread
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.thoughtcrime.securesms.R
import com.polli.android.connect.DcEventCenter

/** Compose manual transport (IMAP/SMTP) config form — replaces legacy Java EditRelayActivity. */
class EditRelayActivity : BaseComposeActivity(), DcEventCenter.DcEventDelegate {

    private lateinit var rpc: Rpc
    private var accId = 0
    private var isEditing = false

    private var email by mutableStateOf("")
    private var emailError by mutableStateOf(false)
    private var password by mutableStateOf("")
    private var imapLogin by mutableStateOf("")
    private var imapServer by mutableStateOf("")
    private var imapServerError by mutableStateOf(false)
    private var imapPort by mutableStateOf("")
    private var imapPortError by mutableStateOf(false)
    private var imapFolder by mutableStateOf("")
    private var smtpLogin by mutableStateOf("")
    private var smtpPassword by mutableStateOf("")
    private var smtpServer by mutableStateOf("")
    private var smtpServerError by mutableStateOf(false)
    private var smtpPort by mutableStateOf("")
    private var smtpPortError by mutableStateOf(false)
    private var imapSecurity by mutableIntStateOf(0)
    private var smtpSecurity by mutableIntStateOf(0)
    private var certCheck by mutableIntStateOf(0)
    private var enforceE2ee by mutableStateOf(false)
    private var showAdvanced by mutableStateOf(false)
    private var showImapFolder by mutableStateOf(false)

    private var providerHint by mutableStateOf<String?>(null)
    private var providerStatus by mutableIntStateOf(0)
    private var provider: DcProvider? = null

    private var progressDialog: LegacyProgressDialog? = null
    private var cancelled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rpc = EngineBridge.getRpc(this)
        accId = EngineBridge.getContext(this).accountId

        val addr = intent.getStringExtra(EXTRA_ADDR)
        val config: EnteredLoginParam? =
            try {
                val relays = rpc.listTransports(accId)
                val found = relays.firstOrNull { addr != null && addr == it.addr }
                if (found == null && relays.isNotEmpty() && addr != null) {
                    Log.e(TAG, "unknown address: $addr")
                    finish()
                    return
                }
                found
            } catch (e: RpcException) {
                Log.e(TAG, "listTransports() failed", e)
                finish()
                return
            }

        isEditing = config != null
        enforceE2ee = EngineBridge.getInt(this, EngineBridge.CONFIG_FORCE_ENCRYPTION) == 1
        var expandAdvanced = !enforceE2ee

        if (config != null) {
            email = config.addr.orEmpty()
            password = config.password.orEmpty()
            imapLogin = config.imapUser.orEmpty()
            imapServer = config.imapServer.orEmpty()
            imapPort = config.imapPort?.toString().orEmpty()
            imapFolder = config.imapFolder.orEmpty()
            showImapFolder = imapFolder.isNotEmpty()
            imapSecurity = socketToInt(config.imapSecurity)
            smtpLogin = config.smtpUser.orEmpty()
            smtpPassword = config.smtpPassword.orEmpty()
            smtpServer = config.smtpServer.orEmpty()
            smtpPort = config.smtpPort?.toString().orEmpty()
            smtpSecurity = socketToInt(config.smtpSecurity)
            certCheck = certToInt(config.certificateChecks)
            expandAdvanced = expandAdvanced ||
                imapLogin.isNotEmpty() || imapServer.isNotEmpty() || imapPort.isNotEmpty() ||
                showImapFolder || imapSecurity != 0 || smtpLogin.isNotEmpty() ||
                smtpPassword.isNotEmpty() || smtpServer.isNotEmpty() || smtpPort.isNotEmpty() ||
                smtpSecurity != 0 || certCheck != 0
        }
        showAdvanced = expandAdvanced

        EngineBridge.getEventCenter(this).addObserver(DcContext.DC_EVENT_CONFIGURE_PROGRESS, this)

        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                EditRelayForm()
            }
        }
    }

    override fun onDestroy() {
        EngineBridge.getEventCenter(this).removeObservers(this)
        super.onDestroy()
    }

    override fun handleEvent(event: DcEvent) {
        if (event.id == DcContext.DC_EVENT_CONFIGURE_PROGRESS) {
            val percent = event.data1Int / 10
            progressDialog?.setMessage(getString(R.string.one_moment) + " $percent%")
        }
    }

    private fun updateProviderInfo() {
        val addr = email
        lifecycleScope.launch {
            val p = withContext(Dispatchers.IO) {
                EngineBridge.getContext(this@EditRelayActivity).getProviderFromEmailWithDns(addr)
            }
            provider = p
            if (p != null) {
                providerHint = p.beforeLoginHint
                providerStatus = p.status
            } else {
                providerHint = null
            }
        }
    }

    private fun maybeCleanProviderInfo() {
        if (provider != null && providerHint != null) {
            provider = null
            providerHint = null
        }
    }

    private fun onProviderLink() {
        val url = provider?.overviewPage
        if (!url.isNullOrEmpty()) {
            PlatformLegacyUtil.showInBrowser(this, url)
        } else {
            Toast.makeText(this, "ErrProviderWithoutUrl", Toast.LENGTH_LONG).show()
        }
    }

    private fun onLogin() {
        val validEmail = EngineBridge.getContext(this).mayBeValidAddr(email)
        if (!validEmail || password.isEmpty()) {
            Toast.makeText(this, R.string.login_error_required_fields, Toast.LENGTH_LONG).show()
            return
        }
        cancelled = false
        setupConfig()
    }

    private fun setupConfig() {
        EngineBridge.captureNextError(this)

        val param = EnteredLoginParam()
        param.addr = email.trim().ifEmpty { null }
        param.password = password.ifEmpty { null }
        param.imapServer = imapServer.trim().ifEmpty { null }
        param.imapPort = imapPort.trim().toIntOrNull()
        param.imapFolder = imapFolder.trim().ifEmpty { null }
        param.imapSecurity = socketFromInt(imapSecurity)
        param.imapUser = imapLogin.ifEmpty { null }
        param.smtpServer = smtpServer.trim().ifEmpty { null }
        param.smtpPort = smtpPort.trim().toIntOrNull()
        param.smtpSecurity = socketFromInt(smtpSecurity)
        param.smtpUser = smtpLogin.ifEmpty { null }
        param.smtpPassword = smtpPassword.ifEmpty { null }
        param.certificateChecks = certFromInt(certCheck)
        val forceEncryption = if (enforceE2ee) "1" else "0"

        progressDialog?.dismiss()
        progressDialog =
            PlatformDialogs.createProgressDialog(this).apply {
                setMessage(getString(R.string.one_moment))
                setCanceledOnTouchOutside(false)
                setCancelable(false)
                setButton(
                    android.content.DialogInterface.BUTTON_NEGATIVE,
                    getString(android.R.string.cancel),
                ) { _, _ ->
                    cancelled = true
                    EngineBridge.getContext(this@EditRelayActivity).stopOngoingProcess()
                }
                show()
            }

        Thread {
            try {
                rpc.setConfig(accId, EngineBridge.CONFIG_FORCE_ENCRYPTION, forceEncryption)
                rpc.addOrUpdateTransport(accId, param)
                EngineBridge.endCaptureNextError(this)
                PlatformThread.runOnMain {
                    progressDialog?.dismiss()
                    startActivity(AppNav.homeIntent(applicationContext))
                    finish()
                }
            } catch (e: RpcException) {
                EngineBridge.endCaptureNextError(this)
                if (!cancelled) {
                    PlatformThread.runOnMain {
                        progressDialog?.dismiss()
                        OnboardingErrors.maybeShowConfigurationError(this, e.message)
                    }
                }
            }
        }.start()
    }

    @Composable
    private fun EditRelayForm() {
        val scroll = rememberScrollState()
        Box(modifier = Modifier.fillMaxSize().background(PolliColors.Black)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(top = AppInsets.statusBarTop() + 8.dp)
                    .padding(bottom = AppInsets.navigationBarBottom() + 32.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundBackButton(onClick = { finish() })
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(
                            if (isEditing) R.string.edit_transport else R.string.manual_account_setup_option,
                        ),
                        color = PolliColors.White85,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onLogin() }) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = PolliColors.White85)
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    RelayField(
                        value = email,
                        onValueChange = { email = it; maybeCleanProviderInfo() },
                        label = stringResource(R.string.email_address),
                        enabled = !isEditing,
                        isError = emailError,
                        onFocusLost = {
                            emailError = email.isNotEmpty() &&
                                !EngineBridge.getContext(this@EditRelayActivity).mayBeValidAddr(email)
                            updateProviderInfo()
                        },
                    )
                    ProviderHint()
                    RelayField(
                        value = password,
                        onValueChange = { password = it },
                        label = stringResource(R.string.existing_password),
                        isPassword = true,
                    )

                    AdvancedToggle()

                    if (showAdvanced) {
                        RelayField(imapLogin, { imapLogin = it }, stringResource(R.string.login_imap_login))
                        RelayField(
                            imapServer, { imapServer = it }, stringResource(R.string.login_imap_server),
                            isError = imapServerError,
                            onFocusLost = { imapServerError = !isServerValid(imapServer) },
                        )
                        RelayField(
                            imapPort, { imapPort = it }, stringResource(R.string.login_imap_port),
                            isError = imapPortError,
                            onFocusLost = { imapPortError = !isPortValid(imapPort) },
                        )
                        SecurityDropdown(stringResource(R.string.login_imap_security), imapSecurity) { imapSecurity = it }
                        if (showImapFolder) {
                            RelayField(imapFolder, { imapFolder = it }, "IMAP folder")
                        }
                        RelayField(smtpLogin, { smtpLogin = it }, stringResource(R.string.login_smtp_login))
                        RelayField(smtpPassword, { smtpPassword = it }, stringResource(R.string.login_smtp_password), isPassword = true)
                        RelayField(
                            smtpServer, { smtpServer = it }, stringResource(R.string.login_smtp_server),
                            isError = smtpServerError,
                            onFocusLost = { smtpServerError = !isServerValid(smtpServer) },
                        )
                        RelayField(
                            smtpPort, { smtpPort = it }, stringResource(R.string.login_smtp_port),
                            isError = smtpPortError,
                            onFocusLost = { smtpPortError = !isPortValid(smtpPort) },
                        )
                        SecurityDropdown(stringResource(R.string.login_smtp_security), smtpSecurity) { smtpSecurity = it }
                        CertCheckDropdown(certCheck) { certCheck = it }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.enforce_e2ee),
                                color = PolliColors.White85,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = enforceE2ee,
                                onCheckedChange = { enforceE2ee = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = PolliColors.White),
                            )
                        }

                        Text(
                            stringResource(R.string.pref_view_log),
                            color = PolliColors.White66,
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .clickable { startActivity(Intent(this@EditRelayActivity, LogViewActivity::class.java)) },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ProviderHint() {
        val hint = providerHint ?: return
        val bg: Color
        val fg: Color
        when (providerStatus) {
            DcProvider.DC_PROVIDER_STATUS_PREPARATION -> {
                bg = colorResource(R.color.provider_prep_bg)
                fg = colorResource(R.color.provider_prep_fg)
            }
            DcProvider.DC_PROVIDER_STATUS_BROKEN -> {
                bg = colorResource(R.color.provider_broken_bg)
                fg = colorResource(R.color.provider_broken_fg)
            }
            else -> return
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .clickable { onProviderLink() }
                .padding(12.dp),
        ) {
            Text(hint, color = fg)
        }
    }

    @Composable
    private fun AdvancedToggle() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable { showAdvanced = !showAdvanced },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.login_advanced_hint),
                color = PolliColors.White85,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = PolliColors.White66,
            )
        }
    }

    @Composable
    private fun RelayField(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        enabled: Boolean = true,
        isError: Boolean = false,
        isPassword: Boolean = false,
        onFocusLost: (() -> Unit)? = null,
    ) {
        var wasFocused by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            enabled = enabled,
            isError = isError,
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = relayFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .onFocusChanged { state ->
                    if (wasFocused && !state.isFocused) onFocusLost?.invoke()
                    wasFocused = state.isFocused
                },
        )
    }

    @Composable
    private fun SecurityDropdown(label: String, selected: Int, onSelect: (Int) -> Unit) {
        val options = listOf(stringResource(R.string.automatic), "SSL/TLS", "StartTLS", stringResource(R.string.off))
        LabeledDropdown(label, options, selected, onSelect)
    }

    @Composable
    private fun CertCheckDropdown(selected: Int, onSelect: (Int) -> Unit) {
        val options = listOf(
            stringResource(R.string.automatic),
            stringResource(R.string.strict),
            stringResource(R.string.accept_invalid_certificates),
        )
        LabeledDropdown(stringResource(R.string.login_certificate_checks), options, selected, onSelect)
    }

    @Composable
    private fun LabeledDropdown(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
        var open by remember { mutableStateOf(false) }
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(label, color = PolliColors.White33, style = MaterialTheme.typography.bodySmall)
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { open = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        options.getOrElse(selected) { options.first() },
                        color = PolliColors.White85,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = PolliColors.White66)
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    options.forEachIndexed { index, opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                onSelect(index)
                                open = false
                            },
                        )
                    }
                }
            }
        }
    }

    private fun isServerValid(server: String): Boolean =
        server.isEmpty() ||
            Patterns.DOMAIN_NAME.matcher(server).matches() ||
            Patterns.IP_ADDRESS.matcher(server).matches() ||
            Patterns.WEB_URL.matcher(server).matches() ||
            server == "localhost"

    private fun isPortValid(portString: String): Boolean {
        if (portString.isEmpty()) return true
        val port = portString.toIntOrNull() ?: return false
        return port in 1..65535
    }

    companion object {
        private const val TAG = "EditRelayActivity"
        const val EXTRA_ADDR = "extra_addr"

        fun intent(context: Context): Intent = Intent(context, EditRelayActivity::class.java)
    }
}

@Composable
private fun relayFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PolliColors.White85,
    unfocusedTextColor = PolliColors.White85,
    disabledTextColor = PolliColors.White33,
    focusedBorderColor = PolliColors.White33,
    unfocusedBorderColor = PolliColors.White16,
    disabledBorderColor = PolliColors.White8,
    focusedLabelColor = PolliColors.White66,
    unfocusedLabelColor = PolliColors.White33,
    disabledLabelColor = PolliColors.White33,
    cursorColor = PolliColors.White85,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
)

private fun socketFromInt(position: Int): Socket =
    when (position) {
        0 -> Socket.automatic
        1 -> Socket.ssl
        2 -> Socket.starttls
        3 -> Socket.plain
        else -> Socket.automatic
    }

private fun socketToInt(security: Socket?): Int =
    when (security) {
        Socket.ssl -> 1
        Socket.starttls -> 2
        Socket.plain -> 3
        else -> 0
    }

private fun certFromInt(position: Int): EnteredCertificateChecks =
    when (position) {
        1 -> EnteredCertificateChecks.strict
        2 -> EnteredCertificateChecks.acceptInvalidCertificates
        else -> EnteredCertificateChecks.automatic
    }

private fun certToInt(check: EnteredCertificateChecks?): Int =
    when (check) {
        EnteredCertificateChecks.strict -> 1
        EnteredCertificateChecks.acceptInvalidCertificates -> 2
        else -> 0
    }
