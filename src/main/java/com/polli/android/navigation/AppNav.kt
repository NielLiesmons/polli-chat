package com.polli.android.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcMsg
import com.polli.android.HomeActivity
import com.polli.android.chat.ChatActivity
import com.polli.android.home.ArchiveActivity
import org.thoughtcrime.securesms.BuildConfig
import org.thoughtcrime.securesms.ConversationActivity
import org.thoughtcrime.securesms.ConversationListActivity
import org.thoughtcrime.securesms.ConversationListArchiveActivity
import com.polli.android.newchat.NewConversationActivity

/** Sole routing API — all user-facing navigation goes through here. */
object AppNav {

    @JvmStatic
    fun useLabUi(): Boolean = BuildConfig.POLLI_UI

    @JvmStatic
    fun homeActivityClass(): Class<*> {
        return if (useLabUi()) HomeActivity::class.java else ConversationListActivity::class.java
    }

    @JvmStatic
    fun chatActivityClass(): Class<*> {
        return if (useLabUi()) ChatActivity::class.java else ConversationActivity::class.java
    }

    @JvmStatic
    fun homeIntent(context: Context): Intent {
        return Intent(context, homeActivityClass()).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    @JvmStatic
    fun homeIntentWithAccount(context: Context, accountId: Int, clearNotifications: Boolean): Intent {
        return homeIntent(context).apply {
            putExtra(ConversationListActivity.ACCOUNT_ID_EXTRA, accountId)
            if (clearNotifications) {
                putExtra(ConversationListActivity.CLEAR_NOTIFICATIONS, true)
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun chatIntent(
        context: Context,
        chatId: Int,
        accountId: Int = -1,
        draftText: String? = null,
        startingPosition: Int = -1,
        fromArchived: Boolean = false,
    ): Intent {
        val intent = Intent(context, chatActivityClass())
        intent.putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId)
        if (accountId >= 0) {
            intent.putExtra(ConversationActivity.ACCOUNT_ID_EXTRA, accountId)
        }
        draftText?.let { intent.putExtra(ConversationActivity.TEXT_EXTRA, it) }
        if (startingPosition >= 0) {
            intent.putExtra(ConversationActivity.STARTING_POSITION_EXTRA, startingPosition)
        }
        if (fromArchived) {
            intent.putExtra(ConversationActivity.FROM_ARCHIVED_CHATS_EXTRA, true)
        }
        return intent
    }

    @JvmStatic
    fun openHome(context: Context) {
        context.startActivity(homeIntent(context))
    }

    @JvmStatic
    fun openChat(context: Context, chatId: Int) {
        context.startActivity(chatIntent(context, chatId))
    }

    @JvmStatic
    @JvmOverloads
    fun openChatWithExtras(
        context: Context,
        chatId: Int,
        accountId: Int = -1,
        draftText: String? = null,
        startingPosition: Int = -1,
        fromArchived: Boolean = false,
    ) {
        context.startActivity(
            chatIntent(context, chatId, accountId, draftText, startingPosition, fromArchived),
        )
    }

    @JvmStatic
    fun openNewConversation(context: Context) {
        if (useLabUi()) {
            context.startActivity(NewConversationActivity.intent(context))
        } else {
            context.startActivity(Intent(context, org.thoughtcrime.securesms.NewConversationActivity::class.java))
        }
    }

    @JvmStatic
    fun archiveIntent(context: Context): Intent {
        return if (useLabUi()) {
            Intent(context, ArchiveActivity::class.java)
        } else {
            Intent(context, ConversationListArchiveActivity::class.java)
        }
    }

    @JvmStatic
    fun openArchive(context: Context) {
        context.startActivity(archiveIntent(context))
    }

    @JvmStatic
    fun loadChannelPosts(dcContext: DcContext, chatId: Int): List<DcMsg> {
        val ids = dcContext.getChatMsgs(chatId, 0, 0) ?: return emptyList()
        val posts = ArrayList<DcMsg>()
        for (id in ids) {
            if (id <= DcMsg.DC_MSG_ID_DAYMARKER) continue
            val msg = dcContext.getMsg(id)
            if (!msg.isOk || msg.isInfo) continue
            val text = msg.text?.trim().orEmpty()
            if (text.isEmpty() && !msg.hasFile()) continue
            posts.add(msg)
        }
        return posts
    }

    /** Unique URI for notification pending intents. */
    @JvmStatic
    fun chatIntentUri(accountId: Int, chatId: Int): Uri {
        return Uri.parse("polli://chat/$accountId/$chatId")
    }
}
