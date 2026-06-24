package com.polli.android.profiles

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.b44t.messenger.DcContact
import com.polli.android.BaseComposeActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.settings.AppSettingsActivity
import com.polli.android.settings.UiScalePreset
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.theme.LabTheme
import com.polli.android.ui.LabAvatar
import com.polli.android.ui.AppInsets
import com.polli.android.ui.AppModal
import com.polli.android.ui.ModalSectionLabel
import com.polli.android.ui.RoundBackButton
import com.polli.android.ui.ShellDivider
import org.thoughtcrime.securesms.connect.DcHelper

class ProfilesActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = remember { AppPrefs(this@ProfilesActivity) }
            var uiScaleRevision by remember { mutableIntStateOf(0) }
            LabTheme(prefs = prefs, uiScaleRevision = uiScaleRevision) {
                ProfilesScreen(
                    prefs = prefs,
                    onScaleChanged = { uiScaleRevision++ },
                    onBack = { finish() },
                    onEditProfile = {
                        startActivity(ProfileEditActivity.intent(this@ProfilesActivity))
                    },
                    onOpenDcSettings = {
                        startActivity(AppSettingsActivity.intent(this@ProfilesActivity))
                    },
                )
            }
        }
    }
}

@Composable
fun ProfilesScreen(
    prefs: AppPrefs,
    onScaleChanged: () -> Unit = {},
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenDcSettings: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dc = remember { DcHelper.getContext(context) }
    val accounts = remember { DcHelper.getAccounts(context) }
    val displayName = dc.getConfig(DcHelper.CONFIG_DISPLAY_NAME).ifBlank { "Profile" }
    val addr = dc.getConfig(DcHelper.CONFIG_CONFIGURED_ADDRESS)
    var showAppearanceModal by remember { mutableStateOf(false) }
    var showChatSettingsModal by remember { mutableStateOf(false) }
    val headerTop = AppInsets.statusBarTop() + 9.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = headerTop + 48.dp,
                    bottom = AppInsets.navigationBarBottom() + 32.dp,
                ),
        ) {
            ShellDivider(screenPad = 0.dp)
            ActiveProfileCard(
                name = displayName,
                subtitle = addr,
                onClick = onEditProfile,
                modifier = Modifier.padding(14.dp),
            )
            ShellDivider(screenPad = 0.dp)

            SettingsRowItem(
                title = "Appearance",
                description = "${prefs.uiScalePreset.label} · ${if (prefs.respectSystemScale) "System text size" else "Fixed text size"}",
                onClick = { showAppearanceModal = true },
            )
            ShellDivider(screenPad = 0.dp)
            SettingsRowItem(
                title = "Chat & notifications",
                description = "Account settings",
                onClick = { showChatSettingsModal = true },
            )
            ShellDivider(screenPad = 0.dp)

            Text(
                "Accounts",
                color = LabColors.White33,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 14.dp, top = 16.dp, bottom = 8.dp),
            )
            accounts.getAll().forEach { accountId ->
                val acc = accounts.getAccount(accountId)
                val name = acc.getConfig(DcHelper.CONFIG_DISPLAY_NAME)
                    .ifBlank { acc.getConfig(DcHelper.CONFIG_CONFIGURED_ADDRESS) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LabAvatar(
                        name = name,
                        seed = name,
                        size = LabDimens.AvatarSize,
                        contactId = DcContact.DC_CONTACT_ID_SELF,
                        dcContext = acc,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(name, color = LabColors.White66, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(LabColors.Black.copy(alpha = 0.85f))
                .border(
                    width = LabDimens.ShellBorderWidth,
                    color = LabColors.White8,
                    shape = RoundedCornerShape(0.dp),
                )
                .padding(top = headerTop, start = 14.dp, end = 14.dp, bottom = 9.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundBackButton(onClick = onBack)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Settings", color = LabColors.White, style = MaterialTheme.typography.titleLarge)
            }
        }
    }

    if (showAppearanceModal) {
        AppearanceSettingsModal(
            prefs = prefs,
            onDismiss = { showAppearanceModal = false },
            onChanged = onScaleChanged,
        )
    }
    if (showChatSettingsModal) {
        AppModal(
            onDismiss = { showChatSettingsModal = false },
            title = "Chat & notifications",
            description = "Open Polli settings for notifications, media, and privacy options.",
        ) {
            SettingsLinkButton(title = "Open settings", onClick = {
                showChatSettingsModal = false
                onOpenDcSettings()
            })
        }
    }
}

@Composable
private fun AppearanceSettingsModal(
    prefs: AppPrefs,
    onDismiss: () -> Unit,
    onChanged: () -> Unit = {},
) {
    var scale by remember { mutableStateOf(prefs.uiScalePreset) }
    var respectSystem by remember { mutableStateOf(prefs.respectSystemScale) }
    AppModal(
        onDismiss = onDismiss,
        title = "Appearance",
        description = "Text size and display preferences for this device.",
    ) {
        ModalSectionLabel("Text size")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UiScalePreset.entries.forEach { preset ->
                ScalePill(
                    label = preset.label,
                    selected = scale == preset,
                    onClick = {
                        scale = preset
                        prefs.uiScalePreset = preset
                        onChanged()
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ModalSectionLabel("System")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Match device text size",
                color = LabColors.White85,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = respectSystem,
                onCheckedChange = {
                    respectSystem = it
                    prefs.respectSystemScale = it
                    onChanged()
                },
            )
        }
    }
}

@Composable
private fun SettingsRowItem(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(title, color = LabColors.White85, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
        Text(description, color = LabColors.White33, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SettingsLinkButton(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        color = LabColors.BlurpleLight,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun ActiveProfileCard(
    name: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LabColors.Gray)
            .border(LabDimens.ShellBorderWidth, LabColors.ShellBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        LabAvatar(
            name = name,
            seed = subtitle.ifBlank { name },
            size = 56.dp,
            contactId = DcContact.DC_CONTACT_ID_SELF,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(name, color = LabColors.White85, style = MaterialTheme.typography.titleMedium)
        if (subtitle.isNotBlank()) {
            Text(subtitle, color = LabColors.White33, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ScalePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) LabColors.Blurple else LabColors.White8
    Text(
        text = label,
        color = if (selected) LabColors.White else LabColors.White66,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(17.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
