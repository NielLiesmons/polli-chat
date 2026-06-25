package com.polli.android.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcMsg
import com.polli.android.HomeActivity
import com.polli.android.chat.ChatActivity
import com.polli.android.home.ArchiveActivity
import com.polli.android.media.ChatAllMediaActivity
import com.polli.android.media.MediaPreviewActivity
import com.polli.android.newchat.NewConversationActivity
import com.polli.android.onboarding.AccountSetupActivity
import com.polli.android.onboarding.WelcomeActivity
import com.polli.android.qr.QrHubActivity
import com.polli.android.profiles.ProfilesActivity
import org.thoughtcrime.securesms.BuildConfig
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.ConversationActivity
import org.thoughtcrime.securesms.ConversationListActivity
import org.thoughtcrime.securesms.ConversationListArchiveActivity
import org.thoughtcrime.securesms.ApplicationPreferencesActivity
import org.thoughtcrime.securesms.ProfileActivity
import org.thoughtcrime.securesms.WebxdcActivity
import org.thoughtcrime.securesms.MediaPreviewActivity as LegacyMediaPreviewActivity
import org.thoughtcrime.securesms.AllMediaActivity as LegacyAllMediaActivity
import org.thoughtcrime.securesms.InstantOnboardingActivity
import org.thoughtcrime.securesms.qr.QrActivity

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
        context.applyChatOpenTransitionIfLab()
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
        context.applyChatOpenTransitionIfLab()
    }

    private fun Context.applyChatOpenTransitionIfLab() {
        if (useLabUi() && this is Activity) {
            overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out)
        }
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
    fun welcomeIntent(context: Context): Intent =
        if (useLabUi()) WelcomeActivity.intent(context) else Intent(context, org.thoughtcrime.securesms.WelcomeActivity::class.java)

    @JvmStatic
    fun openWelcome(context: Context) {
        context.startActivity(welcomeIntent(context))
    }

    @JvmStatic
    fun accountSetupIntent(context: Context): Intent =
        if (useLabUi()) AccountSetupActivity.intent(context) else Intent(context, InstantOnboardingActivity::class.java)

    @JvmStatic
    fun openAccountSetup(context: Context) {
        context.startActivity(accountSetupIntent(context))
    }

    @JvmStatic
    fun qrIntent(context: Context): Intent =
        if (useLabUi()) QrHubActivity.intent(context) else Intent(context, QrActivity::class.java)

    @JvmStatic
    fun openQr(context: Context) {
        context.startActivity(qrIntent(context))
    }

    @JvmStatic
    fun mediaPreviewIntent(context: Context, messageId: Int): Intent =
        if (useLabUi()) {
            MediaPreviewActivity.intent(context, messageId)
        } else {
            Intent(context, LegacyMediaPreviewActivity::class.java).apply {
                putExtra(LegacyMediaPreviewActivity.DC_MSG_ID, messageId)
            }
        }

    @JvmStatic
    fun openMediaPreview(context: Context, messageId: Int) {
        context.startActivity(mediaPreviewIntent(context, messageId))
    }

    @JvmStatic
    fun allMediaIntent(context: Context, chatId: Int): Intent =
        if (useLabUi()) {
            ChatAllMediaActivity.intent(context, chatId)
        } else {
            Intent(context, LegacyAllMediaActivity::class.java).apply {
                putExtra(LegacyAllMediaActivity.CHAT_ID_EXTRA, chatId)
            }
        }

    @JvmStatic
    fun openAllMedia(context: Context, chatId: Int) {
        context.startActivity(allMediaIntent(context, chatId))
    }

    @JvmStatic
    fun settingsIntent(context: Context): Intent =
        if (useLabUi()) ProfilesActivity.intent(context) else Intent(context, ApplicationPreferencesActivity::class.java)

    @JvmStatic
    fun openSettings(context: Context) {
        context.startActivity(settingsIntent(context))
    }

    @JvmStatic
    fun profileIntent(context: Context, contactId: Int): Intent =
        Intent(context, ProfileActivity::class.java).apply {
            putExtra(ProfileActivity.CONTACT_ID_EXTRA, contactId)
        }

    @JvmStatic
    fun webxdcIntent(context: Context, msgId: Int): Intent {
        val dc = org.thoughtcrime.securesms.connect.DcHelper.getContext(context)
        return Intent(context, WebxdcActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("accountId", dc.accountId)
            putExtra("appMessageId", msgId)
            putExtra("hideActionBar", false)
            putExtra("href", "")
        }
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
