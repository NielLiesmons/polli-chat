package com.polli.android.bridge

import android.content.Context
import android.util.Log
import com.b44t.messenger.DcChat
import com.b44t.messenger.DcChatlist
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcLot
import com.polli.core.chat.ChatCategory
import org.thoughtcrime.securesms.connect.DcHelper

data class InboxItem(
    val chatId: Int,
    val name: String,
    val preview: String,
    val previewAuthor: String?,
    val updatedAt: Long,
    val unreadCount: Int,
    val category: ChatCategory,
    val profileImage: String?,
    val colorSeed: String,
)

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

    fun load(context: Context, query: String? = null): List<InboxItem> {
        val dcContext = DcHelper.getContext(context)
        val chatlist = dcContext.getChatlist(CHATLIST_FLAGS, query, 0)
        return mapChatlist(dcContext, chatlist)
    }

    fun loadChannels(context: Context): List<InboxItem> {
        return load(context).filter { it.category == ChatCategory.Channel }
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
            // ConversationListAdapter only renders threads with id > DC_CHAT_ID_LAST_SPECIAL
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
            val (preview, previewAuthor) = summaryPreview(summary)
            val unread = dcContext.getFreshMsgCount(chatId).coerceAtLeast(0)
            out.add(
                InboxItem(
                    chatId = chatId,
                    name = chat.getName().orEmpty(),
                    preview = preview,
                    previewAuthor = previewAuthor,
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

    /** Matches [org.thoughtcrime.securesms.connect.DcHelper.getThreadRecord] preview layout. */
    fun summaryPreview(summary: DcLot): Pair<String, String?> {
        val text1 = summary.getText1()?.trim().orEmpty()
        val text2 = summary.getText2()?.trim().orEmpty()
        return when {
            text1.isNotEmpty() && text2.isNotEmpty() -> text2 to text1
            text2.isNotEmpty() -> text2 to null
            text1.isNotEmpty() -> text1 to null
            else -> "" to null
        }
    }

    /**
     * Polli tab routing — mirrors the first working fork mapper, aligned with DC types:
     * - Broadcasts / mailing lists → story row (Channel)
     * - Multi-user groups → Spaces
     * - Everything else (1:1, contact requests, …) → Mail
     *
     * Uses explicit Java getters (not Kotlin synthetic props) for JNI types.
     */
    fun categorize(chat: DcChat): ChatCategory {
        if (chat.isDeviceTalk || chat.isSelfTalk) {
            return ChatCategory.Skip
        }
        if (chat.isOutBroadcast() || chat.isInBroadcast() || chat.isMailingList()) {
            return ChatCategory.Channel
        }
        val type = chat.getType()
        return when {
            type == DcChat.DC_CHAT_TYPE_GROUP -> ChatCategory.Space
            type == DcChat.DC_CHAT_TYPE_SINGLE -> ChatCategory.Mail
            // Fallback: never drop a real chat because core returned type 0, etc.
            chat.isMultiUser() -> ChatCategory.Space
            else -> ChatCategory.Mail
        }
    }
}
