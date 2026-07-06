@file:OptIn(ExperimentalFoundationApi::class)

package com.polli.android.profiles

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.b44t.messenger.DcContact
import com.polli.android.theme.PolliColors
import com.polli.android.ui.AppInsets
import com.polli.android.ui.PolliAvatar
import com.polli.android.ui.RoundBackButton
import com.polli.android.ui.ShellDivider
import org.thoughtcrime.securesms.connect.DcHelper
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProfileDetailScreen(
    state: ProfileDetailUiState,
    selectedMemberIds: Set<Int>,
    onBack: () -> Unit,
    onAvatarClick: () -> Unit,
    onAllMediaClick: () -> Unit,
    onSendMessageClick: () -> Unit,
    onIntroducedByClick: () -> Unit,
    onMemberClick: (Int) -> Unit,
    onMemberLongClick: (Int) -> Unit,
    onSharedChatClick: (Int) -> Unit,
    onStatusLongClick: () -> Unit,
    onRemoveSelectedMembers: () -> Unit = {},
) {
    val context = LocalContext.current
    val dc = androidx.compose.runtime.remember { DcHelper.getContext(context) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(PolliColors.Black)
                .verticalScroll(rememberScrollState())
                .padding(top = AppInsets.statusBarTop() + 8.dp)
                .padding(bottom = AppInsets.navigationBarBottom() + 24.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            Text(state.screenTitle, color = PolliColors.White85, style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PolliAvatar(
                name = state.displayName.ifBlank { "?" },
                seed = state.seed,
                size = 88.dp,
                chatId = state.chatId.takeIf { it > 0 },
                contactId = state.contactId.takeIf { it > 0 },
                dcContext = dc,
                onClick = onAvatarClick,
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                state.displayName,
                color = PolliColors.White85,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (!state.statusText.isNullOrBlank()) {
            ProfileInfoText(
                text = state.statusText,
                onLongClick = onStatusLongClick,
            )
        } else {
            ShellDivider(screenPad = 0.dp)
        }

        if (state.showAllMedia) {
            ProfileActionRow("Apps & media", onClick = onAllMediaClick)
            ShellDivider(screenPad = 0.dp)
        }
        if (state.showSendMessage) {
            ProfileActionRow("Send message", onClick = onSendMessageClick)
            ShellDivider(screenPad = 0.dp)
        }
        if (!state.lastSeenText.isNullOrBlank()) {
            ProfileMetaRow(state.lastSeenText)
            ShellDivider(screenPad = 0.dp)
        }
        if (state.blocked) {
            ProfileMetaRow("Contact blocked")
            ShellDivider(screenPad = 0.dp)
        }
        if (!state.introducedByLabel.isNullOrBlank()) {
            ProfileActionRow(state.introducedByLabel, onClick = onIntroducedByClick)
            ShellDivider(screenPad = 0.dp)
        }

        if (selectedMemberIds.isNotEmpty()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(PolliColors.Gray)
                        .clickable(onClick = onRemoveSelectedMembers)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Remove ${selectedMemberIds.size} member(s)",
                    color = PolliColors.Rouge,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            ShellDivider(screenPad = 0.dp)
        }

        if (state.members.isNotEmpty()) {
            ShellDivider(screenPad = 0.dp)
            for (member in state.members) {
                val selected = selectedMemberIds.contains(member.contactId)
                ProfileMemberRowView(
                    member = member,
                    selected = selected,
                    onClick = { onMemberClick(member.contactId) },
                    onLongClick = { onMemberLongClick(member.contactId) },
                )
                ShellDivider(screenPad = 0.dp)
            }
        }

        if (state.sharedChats.isNotEmpty()) {
            Text(
                "Shared chats",
                color = PolliColors.White33,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            )
            for (chat in state.sharedChats) {
                ProfileSharedChatRow(
                    title = chat.title,
                    subtitle = chat.subtitle,
                    onClick = { onSharedChatClick(chat.chatId) },
                )
                ShellDivider(screenPad = 0.dp)
            }
        }
    }
}

@Composable
private fun ProfileActionRow(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = PolliColors.White85,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Composable
private fun ProfileMetaRow(text: String) {
    Text(
        text = text,
        color = PolliColors.White33,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun ProfileInfoText(
    text: String,
    onLongClick: () -> Unit,
) {
    Text(
        text = text,
        color = PolliColors.White66,
        style = MaterialTheme.typography.bodyMedium,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PolliColors.Gray33)
                .padding(14.dp)
                .combinedClickable(onLongClick = onLongClick, onClick = {}),
    )
}

@Composable
private fun ProfileMemberRowView(
    member: ProfileMemberRow,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val dc = androidx.compose.runtime.remember { DcHelper.getContext(context) }
    val isSpecial =
        member.contactId == DcContact.DC_CONTACT_ID_ADD_MEMBER ||
            member.contactId == DcContact.DC_CONTACT_ID_QR_INVITE
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (selected) PolliColors.Gray else PolliColors.Black)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isSpecial) {
            PolliAvatar(
                name = member.name,
                seed = member.addr ?: member.name,
                size = 40.dp,
                contactId = member.contactId,
                dcContext = dc,
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(member.name, color = PolliColors.White85, style = MaterialTheme.typography.bodyMedium)
            if (!member.addr.isNullOrBlank()) {
                Text(member.addr, color = PolliColors.White33, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProfileSharedChatRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(title, color = PolliColors.White85, style = MaterialTheme.typography.bodyMedium)
        if (subtitle.isNotBlank()) {
            Text(subtitle, color = PolliColors.White33, style = MaterialTheme.typography.bodySmall)
        }
    }
}
