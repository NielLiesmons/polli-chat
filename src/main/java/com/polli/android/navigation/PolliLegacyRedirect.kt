package com.polli.android.navigation

import android.app.Activity
import android.content.Intent
import com.polli.domain.navigation.ChatIntentExtras
import org.thoughtcrime.securesms.ConversationActivity
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
}
