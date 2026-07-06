package com.polli.android.profiles

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import com.polli.android.media.ImageEditLauncher
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.theme.accent
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import com.polli.android.ui.SelfAvatar
import com.polli.android.platform.EngineBridge
import com.polli.android.platform.PlatformAvatars
import java.io.IOException

class ProfileEditActivity : BaseComposeActivity() {
    private var avatarRevision by mutableIntStateOf(0)

    private val imageEditor = ImageEditLauncher(
        activity = this,
        onEdited = { uri -> saveAvatarFromUri(uri) },
    )

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageEditor.launch(uri, cropAvatar = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = AppPrefs(this)
        setContent {
            val revision = avatarRevision
            PolliTheme(prefs = prefs) {
                ProfileEditScreen(
                    avatarRevision = revision,
                    onBack = { finish() },
                    onChangeAvatar = { pickAvatar.launch("image/*") },
                )
            }
        }
    }

    private fun saveAvatarFromUri(uri: Uri) {
        try {
            val decoded = contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream) ?: return
            val scaled = Bitmap.createScaledBitmap(
                decoded,
                PlatformAvatars.AVATAR_SIZE,
                PlatformAvatars.AVATAR_SIZE,
                true,
            )
            PlatformAvatars.setSelfAvatar(this, scaled)
            avatarRevision++
        } catch (_: IOException) {
            // Same failure mode as legacy profile flows — keep existing avatar.
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, ProfileEditActivity::class.java)
    }
}

@Composable
fun ProfileEditScreen(
    avatarRevision: Int,
    onBack: () -> Unit,
    onChangeAvatar: () -> Unit,
) {
    val context = LocalContext.current
    val dc = remember { EngineBridge.getContext(context) }
    var displayName by remember {
        mutableStateOf(dc.getConfig(EngineBridge.CONFIG_DISPLAY_NAME))
    }
    val addr = dc.getConfig(EngineBridge.CONFIG_CONFIGURED_ADDRESS)

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
            Text("Edit profile", color = PolliColors.White85, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.padding(24.dp))
        key(avatarRevision) {
            SelfAvatar(
                name = displayName.ifBlank { addr },
                size = 72.dp,
                onClick = onChangeAvatar,
            )
        }
        Text(
            "Change photo",
            color = PolliColors.White33,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(onClick = onChangeAvatar),
        )
        Spacer(modifier = Modifier.padding(16.dp))
        BasicTextField(
            value = displayName,
            onValueChange = { displayName = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PolliColors.Gray33)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = PolliColors.White85),
            cursorBrush = SolidColor(PolliColors.White),
            decorationBox = { inner ->
                if (displayName.isEmpty()) Text("Display name", color = PolliColors.White33)
                inner()
            },
        )
        Text(addr, color = PolliColors.White33, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.padding(24.dp))
        TextButton(
            onClick = {
                dc.setConfig(EngineBridge.CONFIG_DISPLAY_NAME, displayName.trim())
                (context as? ProfileEditActivity)?.finish()
            },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Save", color = accent().light)
        }
    }
}
