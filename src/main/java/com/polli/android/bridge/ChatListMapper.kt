package com.polli.android.bridge

import android.content.Context
import android.util.Log
import com.b44t.messenger.DcChat
import com.b44t.messenger.DcChatlist
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcLot
import com.polli.core.chat.ChatCategorizer
import com.polli.core.chat.ChatCategory
import com.polli.core.chat.ChatKind
import com.polli.core.chat.ChatSummaryFormat
import com.polli.domain.model.InboxItem
import org.thoughtcrime.securesms.connect.DcHelper

/**
 * Maps [DcChatlist] → Polli inbox rows.
 *
 * Iteration matches [org.thoughtcrime.securesms.ConversationListAdapter] /
 * [org.thoughtcrime.securesms.ConversationListFragment.loadChatlist] exactly:
 * same flags, same special-id filter, same summary + chat pairing.
 */
object ChatListMapper {
    private const val TAG = "ChatListMapper"

    /** Same flags as ConversationListFragment (non-archive, non-forwarding inbox). */
    private const val CHATLIST_FLAGS = DcContext.DC_GCL_ADD_ALLDONE_HINT

    fun load(context: Context, query: String? = null): List<InboxItem> =
        loadWithFlags(context, CHATLIST_FLAGS, query)

    fun loadArchived(context: Context, query: String? = null): List<InboxItem> =
        loadWithFlags(context, DcContext.DC_GCL_ARCHIVED_ONLY, query)

    fun loadChannels(context: Context): List<InboxItem> =
        load(context).filter { it.category == ChatCategory.Channel }

    private fun loadWithFlags(context: Context, flags: Int, query: String?): List<InboxItem> {
        val dcContext = DcHelper.getContext(context)
        val chatlist = dcContext.getChatlist(flags, query, 0)
        return mapChatlist(dcContext, chatlist)
    }

    private fun mapChatlist(dcContext: DcContext, chatlist: DcChatlist): List<InboxItem> {
        val out = ArrayList<InboxItem>()
        val count = chatlist.getCnt()
        var skippedSpecial = 0
        var skippedDevice = 0
        var space = 0
        var mail = 0
        var channel = 0

        for (i in 0 until count) {
            val chatId = chatlist.getChatId(i)
            if (chatId <= DcChat.DC_CHAT_ID_LAST_SPECIAL) {
                skippedSpecial++
                continue
            }
            val chat = dcContext.getChat(chatId)
            val category = categorize(chat)
            if (category == ChatCategory.Skip) {
                skippedDevice++
                continue
            }
            when (category) {
                ChatCategory.Space -> space++
                ChatCategory.Mail -> mail++
                ChatCategory.Channel -> channel++
                ChatCategory.Skip -> Unit
            }
            val summary: DcLot = chatlist.getSummary(i, chat)
            val preview = summaryPreview(summary)
            val unread = dcContext.getFreshMsgCount(chatId).coerceAtLeast(0)
            out.add(
                InboxItem(
                    chatId = chatId,
                    name = chat.getName().orEmpty(),
                    preview = preview.text,
                    previewAuthor = preview.author,
                    updatedAt = summary.getTimestamp(),
                    unreadCount = unread,
                    category = category,
                    profileImage = chat.getProfileImage(),
                    colorSeed = chat.getName()?.ifBlank { chatId.toString() } ?: chatId.toString(),
                ),
            )
        }

        Log.i(
            TAG,
            "chatlist cnt=$count mapped=${out.size} space=$space mail=$mail channel=$channel " +
                "skipSpecial=$skippedSpecial skipDevice=$skippedDevice",
        )
        return out
    }

    fun summaryPreview(summary: DcLot) =
        ChatSummaryFormat.preview(summary.getText1(), summary.getText2())

    fun categorize(chat: DcChat): ChatCategory =
        ChatCategorizer.categorize(chat.toKind())

    private fun DcChat.toKind() = ChatKind(
        type = getType(),
        isDeviceTalk = isDeviceTalk,
        isSelfTalk = isSelfTalk,
        isOutBroadcast = isOutBroadcast(),
        isInBroadcast = isInBroadcast(),
        isMailingList = isMailingList(),
        isMultiUser = isMultiUser(),
    )
}
