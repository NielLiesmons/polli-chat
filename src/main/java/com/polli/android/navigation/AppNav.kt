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
import com.polli.android.notes.NoteEditorActivity
import org.thoughtcrime.securesms.BuildConfig
import org.thoughtcrime.securesms.R
import com.polli.domain.navigation.ChatIntentExtras
import com.polli.android.settings.NotificationSettingsActivity
import com.polli.android.profiles.ProfileDetailActivity
import com.polli.android.webxdc.WebxdcActivity

/** Sole routing API — all user-facing navigation goes through here. */
object AppNav {

    @JvmStatic
    fun useLabUi(): Boolean = BuildConfig.POLLI_UI

    @JvmStatic
    fun homeActivityClass(): Class<*> = HomeActivity::class.java

    @JvmStatic
    fun chatActivityClass(): Class<*> = ChatActivity::class.java

    @JvmStatic
    fun homeIntentFromWelcome(context: Context, rawQr: String? = null): Intent {
        return homeIntent(context).apply {
            putExtra(ChatIntentExtras.FROM_WELCOME, true)
            rawQr?.let { putExtra(ChatIntentExtras.FROM_WELCOME_RAW_QR, it) }
        }
    }

    @JvmStatic
    fun openNotificationSettings(context: Context) {
        context.startActivity(NotificationSettingsActivity.intent(context))
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
            putExtra(ChatIntentExtras.ACCOUNT_ID, accountId)
            if (clearNotifications) {
                putExtra(ChatIntentExtras.CLEAR_NOTIFICATIONS, true)
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
        intent.putExtra(ChatIntentExtras.CHAT_ID, chatId)
        if (accountId >= 0) {
            intent.putExtra(ChatIntentExtras.ACCOUNT_ID, accountId)
        }
        draftText?.let { intent.putExtra(ChatIntentExtras.DRAFT_TEXT, it) }
        if (startingPosition >= 0) {
            intent.putExtra(ChatIntentExtras.STARTING_POSITION, startingPosition)
        }
        if (fromArchived) {
            intent.putExtra(ChatIntentExtras.FROM_ARCHIVED, true)
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
        context.startActivity(NewConversationActivity.intent(context))
    }

    @JvmStatic
    fun archiveIntent(context: Context): Intent =
        Intent(context, ArchiveActivity::class.java)

    @JvmStatic
    fun openArchive(context: Context) {
        context.startActivity(archiveIntent(context))
    }

    @JvmStatic
    fun openNewNote(context: Context) {
        context.startActivity(NoteEditorActivity.intent(context))
        context.applyChatOpenTransitionIfLab()
    }

    @JvmStatic
    fun openNoteEditor(context: Context, msgId: Int) {
        context.startActivity(NoteEditorActivity.intent(context, msgId))
        context.applyChatOpenTransitionIfLab()
    }

    @JvmStatic
    fun welcomeIntent(context: Context): Intent = WelcomeActivity.intent(context)

    @JvmStatic
    fun openWelcome(context: Context) {
        context.startActivity(welcomeIntent(context))
    }

    @JvmStatic
    fun accountSetupIntent(context: Context): Intent = AccountSetupActivity.intent(context)

    @JvmStatic
    fun openAccountSetup(context: Context) {
        context.startActivity(accountSetupIntent(context))
    }

    @JvmStatic
    fun qrIntent(context: Context): Intent = QrHubActivity.intent(context)

    @JvmStatic
    fun openQr(context: Context) {
        context.startActivity(qrIntent(context))
    }

    @JvmStatic
    fun mediaPreviewIntent(context: Context, messageId: Int): Intent =
        MediaPreviewActivity.intent(context, messageId)

    @JvmStatic
    fun openMediaPreview(context: Context, messageId: Int) {
        context.startActivity(mediaPreviewIntent(context, messageId))
    }

    @JvmStatic
    fun allMediaIntent(context: Context, chatId: Int): Intent =
        ChatAllMediaActivity.intent(context, chatId)

    @JvmStatic
    fun openAllMedia(context: Context, chatId: Int) {
        context.startActivity(allMediaIntent(context, chatId))
    }

    @JvmStatic
    fun settingsIntent(context: Context): Intent = ProfilesActivity.intent(context)

    @JvmStatic
    fun openSettings(context: Context) {
        context.startActivity(settingsIntent(context))
    }

    @JvmStatic
    fun profileIntent(context: Context, contactId: Int): Intent =
        ProfileDetailActivity.intentContact(context, contactId)

    @JvmStatic
    fun chatProfileIntent(context: Context, chatId: Int): Intent =
        ProfileDetailActivity.intentChat(context, chatId)

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
