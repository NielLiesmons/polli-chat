package com.polli.android.newchat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.b44t.messenger.DcContext
import com.polli.android.BaseComposeActivity
import com.polli.android.navigation.AppNav
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabColors
import com.polli.android.theme.accent
import com.polli.android.theme.LabTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import org.thoughtcrime.securesms.connect.DcHelper
import chat.delta.rpc.RpcException

class GroupCreateActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val broadcast = intent.getBooleanExtra(EXTRA_BROADCAST, false)
        val prefs = AppPrefs(this)
        setContent {
            LabTheme(prefs = prefs) {
                GroupCreateScreen(
                    broadcast = broadcast,
                    onBack = { finish() },
                    onCreated = { chatId ->
                        AppNav.openChat(this, chatId)
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_BROADCAST = "broadcast"

        fun intent(context: Context, broadcast: Boolean): Intent =
            Intent(context, GroupCreateActivity::class.java).apply {
                putExtra(EXTRA_BROADCAST, broadcast)
            }
    }
}

@Composable
fun GroupCreateScreen(
    broadcast: Boolean,
    onBack: () -> Unit,
    onCreated: (Int) -> Unit,
) {
    val context = LocalContext.current
    val dc = remember { DcHelper.getContext(context) }
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.Black)
            .padding(top = AppInsets.statusBarTop())
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundBackButton(onClick = onBack)
            Spacer(modifier = Modifier.padding(12.dp))
            Text(
                if (broadcast) "New broadcast" else "New group",
                color = LabColors.White85,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(modifier = Modifier.padding(16.dp))
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LabColors.Gray33)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = LabColors.White85),
            cursorBrush = SolidColor(LabColors.White),
            decorationBox = { inner ->
                if (name.isEmpty()) Text("Name", color = LabColors.White33)
                inner()
            },
        )
        Spacer(modifier = Modifier.padding(24.dp))
        TextButton(
            onClick = {
                val trimmed = name.trim()
                if (trimmed.isEmpty()) return@TextButton
                try {
                    val rpc = DcHelper.getRpc(context)
                    val accId = dc.accountId
                    val chatId = if (broadcast) {
                        rpc.createBroadcast(accId, trimmed)
                    } else {
                        rpc.createGroupChat(accId, trimmed, false)
                    }
                    if (chatId != null && chatId > 0) onCreated(chatId)
                } catch (_: RpcException) {
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Create", color = accent().light, modifier = Modifier.fillMaxWidth())
        }
    }
}
