package com.polli.android.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.android.platform.EngineBridge
import com.polli.android.profiles.ProfileDetailLoader
import com.polli.android.profiles.ProfileMemberRowView
import com.polli.android.theme.PolliColors
import com.polli.android.ui.PolliAvatar
import com.polli.android.ui.ShellDivider

@Composable
fun ChatSpaceInfoTab(
    chatId: Int,
    topPadding: Dp,
) {
    val context = LocalContext.current
    val state =
        remember(chatId) {
            ProfileDetailLoader.load(context, chatId, contactIdIn = 0)
        }
    val dc = remember { EngineBridge.getContext(context) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(PolliColors.Black)
                .verticalScroll(rememberScrollState())
                .padding(top = topPadding)
                .padding(bottom = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PolliAvatar(
                name = state.displayName.ifBlank { "?" },
                seed = state.seed,
                size = 88.dp,
                chatId = state.chatId.takeIf { it > 0 },
                contactId = state.contactId.takeIf { it > 0 },
                dcContext = dc,
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                text = state.displayName,
                color = PolliColors.White85,
                style = MaterialTheme.typography.titleMedium,
            )
            if (!state.statusText.isNullOrBlank()) {
                Spacer(modifier = Modifier.padding(6.dp))
                Text(
                    text = state.statusText,
                    color = PolliColors.White66,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (!state.lastSeenText.isNullOrBlank()) {
            ShellDivider(screenPad = 0.dp)
            Text(
                text = state.lastSeenText,
                color = PolliColors.White33,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        if (state.members.isNotEmpty()) {
            ShellDivider(screenPad = 0.dp)
            Text(
                text = "Members",
                color = PolliColors.White33,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            )
            for (member in state.members) {
                ProfileMemberRowView(
                    member = member,
                    selected = false,
                    onClick = {},
                    onLongClick = {},
                )
                ShellDivider(screenPad = 0.dp)
            }
        }
    }
}
