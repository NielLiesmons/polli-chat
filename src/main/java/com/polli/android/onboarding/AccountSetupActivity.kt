package com.polli.android.onboarding

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import chat.delta.rpc.RpcException
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.polli.android.BaseAppCompatComposeActivity
import com.polli.android.navigation.AppNav
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabColors
import com.polli.android.theme.accent
import com.polli.android.theme.LabTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.LabAvatar
import com.polli.android.ui.RoundBackButton
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.DcEventCenter
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.util.Util
import org.thoughtcrime.securesms.util.views.ProgressDialog
import java.util.concurrent.Executors

class AccountSetupActivity : BaseAppCompatComposeActivity(), DcEventCenter.DcEventDelegate {
    private val executor = Executors.newSingleThreadExecutor()
    private var progressDialog: ProgressDialog? = null
    private var cancelled = false
    private lateinit var providerQrData: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        providerQrData = intent.getStringExtra(EXTRA_QR_DATA) ?: "dcaccount:nine.testrun.org"
        if (DcHelper.getContext(this).isConfigured == 1) {
            finish()
            return
        }
        DcHelper.getEventCenter(this).addObserver(DcContext.DC_EVENT_CONFIGURE_PROGRESS, this)
        val prefs = AppPrefs(this)
        setContent {
            LabTheme(prefs = prefs) {
                AccountSetupScreen(
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onCreate = { name -> createProfile(name) },
                )
            }
        }
    }

    override fun onDestroy() {
        DcHelper.getEventCenter(this).removeObservers(this)
        executor.shutdown()
        super.onDestroy()
    }

    override fun handleEvent(event: DcEvent) {
        if (event.id == DcContext.DC_EVENT_CONFIGURE_PROGRESS) {
            val percent = event.data1Int / 10
            progressDialog?.setMessage(getString(R.string.one_moment) + " $percent%")
        }
    }

    private fun createProfile(name: String) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, R.string.please_enter_name, Toast.LENGTH_LONG).show()
            return
        }
        executor.execute {
            DcHelper.set(this, DcHelper.CONFIG_DISPLAY_NAME, name)
            Util.runOnMain { startQrAccountCreation(providerQrData) }
        }
    }

    private fun startQrAccountCreation(qrCode: String) {
        progressDialog?.dismiss()
        cancelled = false
        progressDialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.one_moment))
            setCanceledOnTouchOutside(false)
            setCancelable(false)
            setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel)) { _, _ ->
                cancelled = true
                DcHelper.getContext(this@AccountSetupActivity).stopOngoingProcess()
            }
            show()
        }
        DcHelper.getEventCenter(this).captureNextError()
        Thread {
            val dc = DcHelper.getContext(this)
            try {
                DcHelper.getRpc(this).addTransportFromQr(dc.accountId, qrCode)
                DcHelper.getEventCenter(this).endCaptureNextError()
                Util.runOnMain {
                    progressDialog?.dismiss()
                    startActivity(AppNav.homeIntent(this))
                    finishAffinity()
                }
            } catch (e: RpcException) {
                DcHelper.getEventCenter(this).endCaptureNextError()
                if (!cancelled) {
                    Util.runOnMain {
                        progressDialog?.dismiss()
                        Toast.makeText(this, e.message ?: getString(R.string.error), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }.start()
    }

    companion object {
        const val EXTRA_QR_DATA = "qr_data"

        fun intent(context: android.content.Context): Intent =
            Intent(context, AccountSetupActivity::class.java)
    }
}

@Composable
fun AccountSetupScreen(
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
) {
    val context = LocalContext.current
    var name by remember {
        mutableStateOf(DcHelper.get(context, DcHelper.CONFIG_DISPLAY_NAME).orEmpty())
    }
    var busy by remember { mutableStateOf(false) }
    val display = name.ifBlank { "?" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.Black)
            .padding(top = AppInsets.statusBarTop() + 8.dp)
            .padding(horizontal = 24.dp)
            .padding(bottom = AppInsets.navigationBarBottom() + 24.dp),
    ) {
        RoundBackButton(onClick = onBack)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Spacer(modifier = Modifier.padding(16.dp))
        Text("Create account", color = LabColors.White85, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(20.dp))
        LabAvatar(name = display, seed = display, size = 88.dp)
        Spacer(modifier = Modifier.padding(20.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = LabColors.White85,
                unfocusedTextColor = LabColors.White85,
                focusedContainerColor = LabColors.Gray33,
                unfocusedContainerColor = LabColors.Gray33,
            ),
        )
        Spacer(modifier = Modifier.padding(24.dp))
        if (busy) {
            CircularProgressIndicator(color = accent().solid, modifier = Modifier.size(32.dp))
        } else {
            TextButton(
                onClick = {
                    busy = true
                    onCreate(name.trim())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent().solid),
            ) {
                Text("Create", color = LabColors.White)
            }
        }
        }
    }
}