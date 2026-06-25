package com.polli.android.profiles

import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.polli.android.settings.AccentPreset
import com.polli.android.settings.AppPrefs
import com.polli.android.ui.UiScaleSlider
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.theme.LabTheme
import com.polli.android.ui.LabAvatar
import com.polli.android.ui.AppInsets
import com.polli.android.ui.AppModal
import com.polli.android.ui.ModalSectionLabel
import com.polli.android.ui.RoundBackButton
import com.polli.android.ui.ShellDivider
import com.polli.ui.theme.AccentThemes
import org.thoughtcrime.securesms.connect.DcHelper

class ProfilesActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs = remember { AppPrefs(this@ProfilesActivity) }
            var themeRevision by remember { mutableIntStateOf(0) }
            LabTheme(prefs = prefs, uiScaleRevision = themeRevision) {
                ProfilesScreen(
                    prefs = prefs,
                    onThemeChanged = { themeRevision++ },
                    onBack = { finish() },
                    onEditProfile = {
                        startActivity(ProfileEditActivity.intent(this@ProfilesActivity))
                    },
                )
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, ProfilesActivity::class.java)
    }
}

@Composable
fun ProfilesScreen(
    prefs: AppPrefs,
    onThemeChanged: () -> Unit = {},
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
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
                description = "${prefs.accentPreset.label} · ${prefs.uiScalePreset.displayLabel}",
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
            onChanged = onThemeChanged,
        )
    }
    if (showChatSettingsModal) {
        ChatSettingsModal(
            onDismiss = { showChatSettingsModal = false },
        )
    }
}

@Composable
private fun AppearanceSettingsModal(
    prefs: AppPrefs,
    onDismiss: () -> Unit,
    onChanged: () -> Unit = {},
) {
    var scale by remember { mutableStateOf(prefs.uiScalePreset) }
    var accentPreset by remember { mutableStateOf(prefs.accentPreset) }
    AppModal(
        onDismiss = onDismiss,
        title = "Appearance",
        description = "Accent color and UI scale for this device.",
    ) {
        ModalSectionLabel("Accent color")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentPreset.entries.forEach { preset ->
                AccentSwatch(
                    preset = preset,
                    selected = accentPreset == preset,
                    onClick = {
                        accentPreset = preset
                        prefs.accentPreset = preset
                        onChanged()
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ModalSectionLabel("UI scale")
        UiScaleSlider(
            value = scale,
            onValueChange = {
                scale = it
                prefs.uiScalePreset = it
                onChanged()
            },
        )
    }
}

@Composable
private fun ChatSettingsModal(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dc = remember { DcHelper.getContext(context) }
    var bccSelf by remember {
        mutableStateOf(dc.getConfigInt(DcHelper.CONFIG_BCC_SELF) != 0)
    }
    var readReceipts by remember {
        mutableStateOf(dc.getConfigInt("mdns_enabled") != 0)
    }
    AppModal(
        onDismiss = onDismiss,
        title = "Chat & notifications",
        description = "Messaging and delivery preferences for this account.",
    ) {
        SettingsToggleRow(
            title = "Read receipts",
            subtitle = "If disabled, you won't see when others read your messages.",
            checked = readReceipts,
            onCheckedChange = {
                readReceipts = it
                dc.setConfigInt("mdns_enabled", if (it) 1 else 0)
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsToggleRow(
            title = "Send copy to self",
            subtitle = "BCC outgoing messages to your saved-messages chat.",
            checked = bccSelf,
            onCheckedChange = {
                bccSelf = it
                dc.setConfigInt(DcHelper.CONFIG_BCC_SELF, if (it) 1 else 0)
            },
        )
    }
}

@Composable
private fun AccentSwatch(
    preset: AccentPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = remember(preset) { AccentThemes.palette(preset) }
    val borderColor = if (selected) LabColors.White else LabColors.White16
    val borderWidth = if (selected) 2.dp else 1.dp
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(palette.gradientBrush())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {}
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = LabColors.White85, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                color = LabColors.White33,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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

