package com.polli.android.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.polli.android.navigation.AppNav
import com.polli.android.navigation.ShareRelay
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliDimens
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.PolliAvatar
import com.polli.android.ui.RoundBackButton
import com.polli.ui.screens.ArchiveScreen

class ArchiveActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                ArchiveHost(
                    onBack = { finish() },
                    onOpenChat = { chatId ->
                        if (ShareRelay.isActive(this)) {
                            ShareRelay.openChat(this, chatId)
                        } else {
                            AppNav.openChat(this, chatId)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ArchiveHost(onBack: () -> Unit, onOpenChat: (Int) -> Unit) {
    val items = rememberArchivedItems()
    val nowSec = remember { System.currentTimeMillis() / 1000 }
    ArchiveScreen(
        items = items,
        nowSec = nowSec,
        onBack = onBack,
        onOpenChat = onOpenChat,
        topInset = AppInsets.statusBarTop(),
        backButton = { RoundBackButton(onClick = onBack) },
        avatar = { item ->
            PolliAvatar(
                name = item.name,
                seed = item.colorSeed,
                size = PolliDimens.AvatarSize,
                chatId = item.chatId,
            )
        },
    )
}
