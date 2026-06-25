package com.polli.android.profiles

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
import com.polli.android.BaseComposeActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabColors
import com.polli.android.theme.accent
import com.polli.android.theme.LabTheme
import com.polli.android.ui.LabAvatar
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import org.thoughtcrime.securesms.connect.DcHelper

class ProfileEditActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = AppPrefs(this)
        setContent {
            LabTheme(prefs = prefs) {
                ProfileEditScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, ProfileEditActivity::class.java)
    }
}

@Composable
fun ProfileEditScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dc = remember { DcHelper.getContext(context) }
    var displayName by remember {
        mutableStateOf(dc.getConfig(DcHelper.CONFIG_DISPLAY_NAME))
    }
    val addr = dc.getConfig(DcHelper.CONFIG_CONFIGURED_ADDRESS)

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
            Text("Edit profile", color = LabColors.White85, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.padding(24.dp))
        LabAvatar(name = displayName.ifBlank { addr }, seed = addr, size = 72.dp)
        Spacer(modifier = Modifier.padding(16.dp))
        BasicTextField(
            value = displayName,
            onValueChange = { displayName = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LabColors.Gray33)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = LabColors.White85),
            cursorBrush = SolidColor(LabColors.White),
            decorationBox = { inner ->
                if (displayName.isEmpty()) Text("Display name", color = LabColors.White33)
                inner()
            },
        )
        Text(addr, color = LabColors.White33, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.padding(24.dp))
        TextButton(
            onClick = {
                dc.setConfig(DcHelper.CONFIG_DISPLAY_NAME, displayName.trim())
                (context as? ProfileEditActivity)?.finish()
            },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Save", color = accent().light)
        }
    }
}
