package com.polli.android.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.b44t.messenger.DcChat
import com.b44t.messenger.DcContext
import com.polli.android.bridge.ChatListMapper
import com.polli.android.bridge.InboxItem
import com.polli.core.chat.ChatCategory
import org.thoughtcrime.securesms.connect.DcHelper

data class ArchiveLinkState(
    val visible: Boolean,
    val unreadCount: Int,
)

fun archiveLinkState(context: android.content.Context): ArchiveLinkState {
    val dc = DcHelper.getContext(context)
    val chatlist = dc.getChatlist(DcContext.DC_GCL_ARCHIVED_ONLY, null, 0)
    val visible = chatlist.getCnt() > 0
    val unread = if (visible) {
        dc.getFreshMsgCount(DcChat.DC_CHAT_ID_ARCHIVED_LINK).coerceAtLeast(0)
    } else {
        0
    }
    return ArchiveLinkState(visible = visible, unreadCount = unread)
}

@Composable
fun rememberArchiveLinkState(): ArchiveLinkState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(archiveLinkState(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state = archiveLinkState(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state
}

fun loadArchived(context: android.content.Context): List<InboxItem> {
    val dc = DcHelper.getContext(context)
    val flags = DcContext.DC_GCL_ARCHIVED_ONLY
    val chatlist = dc.getChatlist(flags, null, 0)
    val out = ArrayList<InboxItem>()
    for (i in 0 until chatlist.getCnt()) {
        val chatId = chatlist.getChatId(i)
        if (chatId <= DcChat.DC_CHAT_ID_LAST_SPECIAL) continue
        val chat = dc.getChat(chatId)
        val category = ChatListMapper.categorize(chat)
        if (category == ChatCategory.Skip) continue
        val summary = chatlist.getSummary(i, chat)
        val (preview, previewAuthor) = ChatListMapper.summaryPreview(summary)
        val unread = dc.getFreshMsgCount(chatId).coerceAtLeast(0)
        out.add(
            InboxItem(
                chatId = chatId,
                name = chat.name ?: "",
                preview = preview,
                previewAuthor = previewAuthor,
                updatedAt = summary.timestamp,
                unreadCount = unread,
                category = category,
                profileImage = chat.profileImage,
                colorSeed = chat.name ?: chatId.toString(),
            ),
        )
    }
    return out
}
