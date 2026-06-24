package com.polli.android.home

import com.b44t.messenger.DcChat
import com.b44t.messenger.DcContext
import com.polli.android.bridge.ChatListMapper
import com.polli.android.bridge.InboxItem
import com.polli.core.chat.ChatCategory
import org.thoughtcrime.securesms.connect.DcHelper

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
