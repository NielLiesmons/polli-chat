package com.polli.android.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.polli.android.BaseComposeActivity
import com.polli.android.icons.PolliIcon
import com.polli.android.icons.PolliIconName
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import org.thoughtcrime.securesms.connect.DcHelper

class AppSettingsActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                AppSettingsScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, AppSettingsActivity::class.java)
    }
}

@Composable
fun AppSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dc = remember { DcHelper.getContext(context) }
    var bccSelf by remember {
        mutableStateOf(dc.getConfigInt(DcHelper.CONFIG_BCC_SELF) != 0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .padding(top = AppInsets.statusBarTop() + 48.dp)
            .padding(bottom = AppInsets.navigationBarBottom() + 32.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundBackButton(onClick = onBack)
            Spacer(modifier = Modifier.padding(12.dp))
            Text("Settings", color = PolliColors.White85, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.padding(16.dp))
        SettingsToggle(
            icon = PolliIconName.Bell,
            title = "Send copy to self",
            checked = bccSelf,
            onChecked = {
                bccSelf = it
                dc.setConfigInt(DcHelper.CONFIG_BCC_SELF, if (it) 1 else 0)
            },
        )
        Spacer(modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun SettingsToggle(
    icon: PolliIconName,
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PolliColors.Gray33)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PolliIcon(icon, 18.dp, PolliColors.White66)
        Spacer(modifier = Modifier.padding(10.dp))
        Text(title, color = PolliColors.White85, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
