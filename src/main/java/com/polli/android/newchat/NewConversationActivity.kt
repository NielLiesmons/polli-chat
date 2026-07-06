package com.polli.android.newchat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.polli.android.BaseComposeActivity
import com.polli.android.icons.PolliIcon
import com.polli.android.icons.PolliIconName
import com.polli.android.navigation.AppNav
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton

class NewConversationActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                NewConversationScreen(
                    onBack = { finish() },
                    onNewOneToOne = {
                        startActivity(ContactPickerActivity.intent(this))
                        finish()
                    },
                    onNewGroup = {
                        startActivity(GroupCreateActivity.intent(this, broadcast = false))
                        finish()
                    },
                    onNewBroadcast = {
                        startActivity(GroupCreateActivity.intent(this, broadcast = true))
                        finish()
                    },
                    onScanQr = {
                        startActivity(AppNav.qrIntent(this))
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, NewConversationActivity::class.java)
    }
}

@Composable
fun NewConversationScreen(
    onBack: () -> Unit,
    onNewOneToOne: () -> Unit,
    onNewGroup: () -> Unit,
    onNewBroadcast: () -> Unit,
    onScanQr: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .padding(top = AppInsets.statusBarTop())
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundBackButton(onClick = onBack)
            Spacer(modifier = Modifier.padding(12.dp))
            Text("New", color = PolliColors.White85, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(24.dp))
        NewAction("New 1:1 chat", PolliIconName.EmojiFill, onNewOneToOne)
        NewAction("New group", PolliIconName.Plus, onNewGroup)
        NewAction("New broadcast", PolliIconName.Bell, onNewBroadcast)
        NewAction("Scan QR code", PolliIconName.Camera, onScanQr)
    }
}

@Composable
private fun NewAction(label: String, icon: PolliIconName, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PolliColors.Gray33)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PolliIcon(icon, 18.dp, PolliColors.White66)
        Spacer(modifier = Modifier.padding(12.dp))
        Text(label, color = PolliColors.White85, style = MaterialTheme.typography.bodyLarge)
    }
}
