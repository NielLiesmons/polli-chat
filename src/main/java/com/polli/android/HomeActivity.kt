package com.polli.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.polli.android.home.HomeScreen
import com.polli.android.navigation.AppNav
import com.polli.android.onboarding.WelcomeActivity
import com.polli.android.profiles.ProfilesActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.stories.ChannelStoriesActivity
import com.polli.android.theme.LabTheme
import org.thoughtcrime.securesms.connect.DcHelper

class HomeActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!DcHelper.isConfigured(this)) {
            startActivity(WelcomeActivity.intent(this))
            finish()
            return
        }
        val prefs = AppPrefs(this)
        setContent {
            LabTheme(prefs = prefs) {
                val dc = DcHelper.getContext(this)
                HomeScreen(
                    profileName = dc.getConfig(DcHelper.CONFIG_DISPLAY_NAME).ifBlank { "Profile" },
                    profileSeed = dc.getConfig(DcHelper.CONFIG_CONFIGURED_ADDRESS).ifBlank { "me" },
                    onProfileClick = {
                        startActivity(Intent(this, ProfilesActivity::class.java))
                    },
                    onPlusClick = { AppNav.openNewConversation(this) },
                    onChatClick = { chatId -> AppNav.openChat(this, chatId) },
                    onChannelClick = { chatId ->
                        val channelIds = com.polli.android.bridge.ChatListMapper
                            .loadChannels(this)
                            .map { it.chatId }
                        startActivity(
                            ChannelStoriesActivity.intent(this, chatId, channelIds),
                        )
                    },
                    onSearch = { /* query applied inside HomeScreen local state */ },
                    onArchiveClick = { AppNav.openArchive(this) },
                )
            }
        }
    }
}
