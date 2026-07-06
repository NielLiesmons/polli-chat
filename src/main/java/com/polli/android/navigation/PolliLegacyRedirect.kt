package com.polli.android.navigation

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.polli.domain.navigation.ChatIntentExtras
import org.thoughtcrime.securesms.ConversationActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.util.DynamicTheme
import org.thoughtcrime.securesms.util.SendRelayedMessageUtil
import org.thoughtcrime.securesms.util.ShareUtil

/** Redirects legacy Java chat/home activities to Polli Compose when [AppNav.useLabUi]. */
object PolliLegacyRedirect {

    @JvmStatic
    fun redirectConversationToPolli(activity: Activity): Boolean {
        if (!AppNav.useLabUi()) return false
        val src = activity.intent ?: Intent()
        val chatId = src.getIntExtra(ConversationActivity.CHAT_ID_EXTRA, -1)
        if (chatId <= 0) {
            activity.finish()
            return true
        }
        val forward = AppNav.chatIntent(
            context = activity,
            chatId = chatId,
            accountId = src.getIntExtra(ConversationActivity.ACCOUNT_ID_EXTRA, -1),
            draftText = src.getStringExtra(ConversationActivity.TEXT_EXTRA),
            startingPosition = src.getIntExtra(ConversationActivity.STARTING_POSITION_EXTRA, -1),
            fromArchived = src.getBooleanExtra(ChatIntentExtras.FROM_ARCHIVED, false) ||
                src.getBooleanExtra(ConversationActivity.FROM_ARCHIVED_CHATS_EXTRA, false),
        )
        ShareUtil.acquireRelayMessageContent(activity, forward)
        activity.startActivity(forward)
        activity.finish()
        return true
    }

    @JvmStatic
    fun redirectConversationListToPolli(activity: Activity): Boolean {
        if (!AppNav.useLabUi()) return false
        if (ShareUtil.isRelayingMessageContent(activity)) return false
        val forward = AppNav.homeIntent(activity).apply {
            action = activity.intent?.action
            data = activity.intent?.data
            activity.intent?.extras?.let { putExtras(it) }
        }
        activity.startActivity(forward)
        activity.finish()
        return true
    }

    /**
     * Polli replacement for [org.thoughtcrime.securesms.ConversationListActivity.openConversation].
     * @return true when handled (caller should not open legacy chat).
     */
    @JvmStatic
    fun openPolliChatFromList(activity: Activity, chatId: Int, startingPosition: Int): Boolean {
        if (!AppNav.useLabUi()) return false
        val dcContext = DcHelper.getContext(activity)
        val fwdAccId = ShareUtil.getForwardedMessageAccountId(activity)
        val chat = dcContext.getChat(chatId)
        if (fwdAccId == dcContext.accountId && chat.isSelfTalk) {
            SendRelayedMessageUtil.immediatelyRelay(activity, chatId)
            Toast.makeText(
                activity,
                DynamicTheme.getCheckmarkEmoji(activity) + " " + activity.getString(R.string.saved),
                Toast.LENGTH_SHORT,
            ).show()
            ShareRelay.finishRelayShell(activity)
            activity.finish()
            return true
        }
        ShareRelay.openChat(activity, chatId, startingPosition)
        return true
    }
}
