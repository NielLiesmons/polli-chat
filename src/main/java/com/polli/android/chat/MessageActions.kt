package com.polli.android.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.polli.android.data.engine.PolliRepositories
import com.polli.android.navigation.AppNav
import com.polli.android.HomeRelayingActivity
import com.polli.android.platform.AttachmentIntents
import com.polli.core.PolliFeatures
import com.polli.domain.model.chat.ChatActionContext
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.ChatMessageRules
import com.polli.android.R
import com.polli.android.webxdc.WebxdcActivity
import com.polli.android.platform.PlatformClipboard
import com.polli.android.platform.PlatformShare
import chat.delta.rpc.RpcException
import java.util.Collections

enum class MessageActionId {
    Reply,
    Copy,
    Share,
    Forward,
    Edit,
    ReplyPrivately,
    Save,
    Unsave,
    ExportAttachment,
    Info,
    Resend,
    AddToHomeScreen,
    Delete,
}

data class MessageAction(
    val id: MessageActionId,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val destructive: Boolean = false,
)

object MessageActions {
    fun available(
        context: Context,
        session: ChatActionContext,
        msgId: Int,
    ): List<MessageAction> {
        val msg = PolliRepositories.messages(context).getMessage(msgId) ?: return emptyList()
        val actions = mutableListOf<MessageAction>()

        if (session.canSend && ChatMessageRules.canReplyToMsg(msg)) {
            actions += MessageAction(
                MessageActionId.Reply,
                R.string.notify_reply_button,
                R.drawable.ic_polli_reply,
            )
        }
        if (session.isEncrypted && session.canSend && ChatMessageRules.canEditMsg(msg)) {
            actions += MessageAction(
                MessageActionId.Edit,
                R.string.edit_message,
                R.drawable.ic_create_white_24dp,
            )
        }
        actions += MessageAction(
            MessageActionId.Copy,
            R.string.menu_copy_text_to_clipboard,
            R.drawable.ic_content_copy_white_24dp,
        )
        if (msg.hasAttachment) {
            actions += MessageAction(
                MessageActionId.Share,
                R.string.menu_share,
                R.drawable.ic_share_white_24dp,
            )
        }
        actions += MessageAction(
            MessageActionId.Forward,
            R.string.menu_forward,
            R.drawable.ic_forward_white_24dp,
        )
        if (session.isMultiUser && !msg.isOutgoing && ChatMessageRules.canReplyToMsg(msg)) {
            actions += MessageAction(
                MessageActionId.ReplyPrivately,
                R.string.reply_privately,
                R.drawable.ic_polli_reply,
            )
        }
        if (msg.savedMessageId > 0) {
            actions += MessageAction(
                MessageActionId.Unsave,
                R.string.unsave,
                R.drawable.baseline_bookmark_remove_24,
            )
        } else if ((msg.hasAttachment || msg.text.isNotBlank()) && !msg.isInfo && !session.isSelfTalk) {
            actions += MessageAction(
                MessageActionId.Save,
                R.string.save,
                R.drawable.baseline_bookmark_border_24,
            )
        }
        if (msg.hasAttachment) {
            actions += MessageAction(
                MessageActionId.ExportAttachment,
                R.string.menu_export_attachments,
                R.drawable.ic_share_white_24dp,
            )
        }
        actions += MessageAction(
            MessageActionId.Info,
            R.string.info,
            R.drawable.ic_help_24dp,
        )
        if (msg.isOutgoing) {
            actions += MessageAction(
                MessageActionId.Resend,
                R.string.resend,
                R.drawable.ic_swap_vert_24dp,
            )
        }
        if (PolliFeatures.WEBXDC_ENABLED && msg.viewType == "Webxdc") {
            actions += MessageAction(
                MessageActionId.AddToHomeScreen,
                R.string.add_to_home_screen,
                R.drawable.ic_apps_24,
            )
        }
        actions += MessageAction(
            MessageActionId.Delete,
            R.string.delete,
            R.drawable.ic_delete_white_24dp,
            destructive = true,
        )
        return actions
    }
}

class MessageActionExecutor(
    private val context: Context,
    private val chatId: Int,
    private val onReply: (ChatMessage) -> Unit,
    private val onEdit: (ChatMessage) -> Unit,
    private val onDeleted: () -> Unit,
) {
    private val messages get() = PolliRepositories.messages(context)

    fun run(actionId: MessageActionId, msgId: Int) {
        val msg = messages.getMessage(msgId) ?: return
        when (actionId) {
            MessageActionId.Reply -> onReply(msg)
            MessageActionId.Copy -> copyMessage(msg)
            MessageActionId.Share -> AttachmentIntents.share(context, msgId)
            MessageActionId.Forward -> forwardMessage(msgId)
            MessageActionId.Edit -> onEdit(msg)
            MessageActionId.ReplyPrivately -> replyPrivately(msgId)
            MessageActionId.Save -> messages.saveMessage(msgId)
            MessageActionId.Unsave -> messages.unsaveMessage(msg.savedMessageId)
            MessageActionId.ExportAttachment ->
                AttachmentIntents.openForView(context, msgId)
            MessageActionId.Info -> showInfo(msgId)
            MessageActionId.Resend -> messages.resendMessages(intArrayOf(msgId))
            MessageActionId.AddToHomeScreen -> {
                if (!PolliFeatures.WEBXDC_ENABLED) return
                val activity = context as? Activity
                if (activity != null) {
                    WebxdcActivity.addToHomeScreen(activity, msgId)
                }
            }
            MessageActionId.Delete -> {
                messages.deleteMessages(intArrayOf(msgId))
                onDeleted()
            }
        }
    }

    private fun copyMessage(msg: ChatMessage) {
        val text = msg.text.trim()
        if (text.isEmpty()) return
        PlatformClipboard.copyText(context, text)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun forwardMessage(msgId: Int) {
        val activity = context as? Activity ?: return
        try {
            val intent = Intent()
            PlatformShare.setForwardingMessageIds(
                intent,
                intArrayOf(msgId),
                PolliRepositories.accounts(context).selectedAccountId,
            )
            HomeRelayingActivity.start(activity, intent)
        } catch (_: RpcException) {
            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun replyPrivately(msgId: Int) {
        val msg = messages.getMessage(msgId) ?: return
        val privateChatId = PolliRepositories.chat(context).createChatByContactId(msg.authorId) ?: return
        messages.setDraft(privateChatId, "", quotedMessageId = msgId)
        AppNav.openChatWithExtras(context, privateChatId)
    }

    private fun showInfo(msgId: Int) {
        val activity = context as? Activity ?: return
        val info = messages.getMessageInfo(msgId) ?: return
        val view = LayoutInflater.from(context).inflate(R.layout.message_details_view, null)
        view.findViewById<TextView>(R.id.details_text).text = info
        AlertDialog.Builder(activity)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
