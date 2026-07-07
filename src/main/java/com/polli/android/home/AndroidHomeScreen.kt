package com.polli.android.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polli.android.data.engine.PolliRepositories
import com.polli.android.notes.Note
import com.polli.android.notes.rememberNotes
import com.polli.android.stories.ChannelStoriesOverlay
import com.polli.android.stories.ChannelStoryRingLogic
import com.polli.android.stories.StoriesViewModel
import com.polli.android.ui.PolliAvatar
import com.polli.android.ui.SelfAvatar
import com.polli.android.theme.PolliDimens
import com.polli.ui.home.HomeNote
import com.polli.ui.home.StoryRingEntry
import com.polli.ui.home.StoryRingStyle
import com.polli.ui.screens.HomeScreen
import com.polli.android.R
import com.polli.android.platform.EngineBridge

/** Android host for the shared [HomeScreen] — supplies engine-backed avatars and story rings. */
@Composable
fun AndroidHomeScreen(
    profileName: String,
    profileSeed: String,
    storiesViewModel: StoriesViewModel? = null,
    shareRelayTitle: String? = null,
    onProfileClick: () -> Unit,
    onPlusClick: () -> Unit,
    onChatClick: (Int) -> Unit,
    onChannelClick: (Int) -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onNewNote: () -> Unit = {},
    onOpenNote: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val chatRepository = remember(context) { PolliRepositories.chat(context) }
    val notes = rememberNotes().map(Note::toHomeNote)

    HomeScreen(
        profileName = profileName,
        profileSeed = profileSeed,
        chatRepository = chatRepository,
        archivedChatsTitle = stringResource(R.string.chat_archived_chats_title),
        notes = notes,
        storyRingLoader = { channels, nowSec ->
            ChannelStoryRingLogic.buildEntries(EngineBridge.getContext(context), channels, nowSec)
                .map { it.toSharedStoryRingEntry() }
        },
        onProfileClick = onProfileClick,
        onPlusClick = onPlusClick,
        onChatClick = onChatClick,
        onChannelClick = onChannelClick,
        onArchiveClick = onArchiveClick,
        onNewNote = onNewNote,
        onOpenNote = onOpenNote,
        shareRelayTitle = shareRelayTitle,
        storiesOverlay =
            if (storiesViewModel != null) {
                { session, onClose ->
                    ChannelStoriesOverlay(
                        session =
                            com.polli.android.stories.StorySession(
                                channelId = session.channelId,
                                channelIds = session.channelIds,
                                launchBounds =
                                    com.polli.android.stories.StoryLaunchBounds(
                                        centerX = session.launchBounds.centerX,
                                        centerY = session.launchBounds.centerY,
                                        size = session.launchBounds.size,
                                    ),
                            ),
                        storiesViewModel = storiesViewModel,
                        onClose = onClose,
                    )
                }
            } else {
                null
            },
        chatAvatar = { item, size ->
            PolliAvatar(
                name = item.name,
                seed = item.colorSeed,
                size = size,
                chatId = item.chatId,
            )
        },
        selfAvatar = { name, size, onClick ->
            SelfAvatar(name = name, size = size, onClick = onClick)
        },
    )
}

private fun Note.toHomeNote() =
    HomeNote(
        msgId = msgId,
        title = title,
        preview = preview,
        body = body,
        timestamp = timestamp,
        hasAttachment = hasAttachment,
    )

private fun com.polli.android.stories.StoryRingEntry.toSharedStoryRingEntry() =
    StoryRingEntry(
        channel = channel,
        style =
            when (style) {
                com.polli.android.stories.StoryRingStyle.Unread -> StoryRingStyle.Unread
                com.polli.android.stories.StoryRingStyle.ReadRecent -> StoryRingStyle.ReadRecent
                com.polli.android.stories.StoryRingStyle.Stale -> StoryRingStyle.Stale
            },
        newestStoryAt = newestStoryAt,
    )
