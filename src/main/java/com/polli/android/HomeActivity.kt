package com.polli.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.viewModels
import com.polli.android.home.HomeScreen
import com.polli.android.navigation.AppNav
import com.polli.android.navigation.ShareRelay
import com.polli.android.onboarding.WelcomeActivity
import com.polli.android.permissions.BackgroundSetup
import com.polli.android.profiles.ProfilesActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.stories.StoriesViewModel
import com.polli.android.theme.LabTheme
import org.thoughtcrime.securesms.connect.DcHelper

class HomeActivity : BaseComposeActivity() {

    private val storiesViewModel: StoriesViewModel by viewModels()
    private var themeRevision by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!DcHelper.isConfigured(this)) {
            startActivity(WelcomeActivity.intent(this))
            finish()
            return
        }
        BackgroundSetup.handleHomeIntent(this, intent)
        setContent {
            val prefs = remember { AppPrefs(this@HomeActivity) }
            val revision = themeRevision
            LabTheme(prefs = prefs, uiScaleRevision = revision) {
                val dc = DcHelper.getContext(this)
                HomeScreen(
                    profileName = dc.getConfig(DcHelper.CONFIG_DISPLAY_NAME).ifBlank { "Profile" },
                    profileSeed = dc.getConfig(DcHelper.CONFIG_CONFIGURED_ADDRESS).ifBlank { "me" },
                    storiesViewModel = storiesViewModel,
                    shareRelayTitle = ShareRelay.relayTitle(this@HomeActivity),
                    onProfileClick = {
                        startActivity(Intent(this, ProfilesActivity::class.java))
                    },
                    onPlusClick = { AppNav.openNewConversation(this) },
                    onChatClick = { chatId ->
                        if (ShareRelay.isActive(this)) {
                            ShareRelay.openChat(this, chatId)
                        } else {
                            AppNav.openChat(this, chatId)
                        }
                    },
                    onSearch = { /* query applied inside HomeScreen local state */ },
                    onArchiveClick = { AppNav.openArchive(this) },
                    onNewNote = { AppNav.openNewNote(this) },
                    onOpenNote = { msgId -> AppNav.openNoteEditor(this, msgId) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        BackgroundSetup.handleHomeIntent(this, intent)
    }

    override fun onResume() {
        super.onResume()
        themeRevision++
        BackgroundSetup.updateReminders(this)
    }
}
