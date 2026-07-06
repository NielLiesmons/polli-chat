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
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcMsg
import com.polli.android.navigation.AppNav
import com.polli.android.HomeRelayingActivity
import org.thoughtcrime.securesms.R
import com.polli.android.webxdc.WebxdcActivity
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.util.ShareUtil.setForwardingMessageIds
import org.thoughtcrime.securesms.util.Util
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
    fun available(context: Context, chatId: Int, msgId: Int): List<MessageAction> {
        val dc = DcHelper.getContext(context)
        val msg = dc.getMsg(msgId)
        if (!msg.isOk) return emptyList()
        val chat = dc.getChat(chatId)
        val actions = mutableListOf<MessageAction>()

        if (chat.canSend() && ChatMessageRules.canReplyToMsg(msg)) {
            actions += MessageAction(
                MessageActionId.Reply,
                R.string.notify_reply_button,
                R.drawable.ic_polli_reply,
            )
        }
        if (chat.isEncrypted && chat.canSend() && ChatMessageRules.canEditMsg(msg)) {
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
        if (msg.hasFile()) {
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
        if (chat.isMultiUser && !msg.isOutgoing && ChatMessageRules.canReplyToMsg(msg)) {
            actions += MessageAction(
                MessageActionId.ReplyPrivately,
                R.string.reply_privately,
                R.drawable.ic_polli_reply,
            )
        }
        if (msg.canSave() && !chat.isSelfTalk) {
            val saved = msg.savedMsgId != 0
            actions += MessageAction(
                if (saved) MessageActionId.Unsave else MessageActionId.Save,
                if (saved) R.string.unsave else R.string.save,
                if (saved) R.drawable.baseline_bookmark_remove_24 else R.drawable.baseline_bookmark_border_24,
            )
        }
        if (msg.hasFile()) {
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
        if (msg.type == DcMsg.DC_MSG_WEBXDC) {
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

/** Message action visibility rules (formerly legacy ConversationFragment). */
object ChatMessageRules {
    fun canReplyToMsg(msg: DcMsg): Boolean = !msg.isInfo

    fun canEditMsg(msg: DcMsg): Boolean =
        msg.isOutgoing &&
            !msg.isInfo &&
            msg.type != DcMsg.DC_MSG_CALL &&
            !msg.hasHtml() &&
            !msg.text.isNullOrEmpty()
}

class MessageActionExecutor(
    private val context: Context,
    private val chatId: Int,
    private val onReply: (DcMsg) -> Unit,
    private val onEdit: (DcMsg) -> Unit,
    private val onDeleted: () -> Unit,
) {
    fun run(actionId: MessageActionId, msgId: Int) {
        val dc = DcHelper.getContext(context)
        val msg = dc.getMsg(msgId)
        if (!msg.isOk) return
        when (actionId) {
            MessageActionId.Reply -> onReply(msg)
            MessageActionId.Copy -> copyMessage(msg)
            MessageActionId.Share -> DcHelper.openForViewOrShare(context, msgId, Intent.ACTION_SEND)
            MessageActionId.Forward -> forwardMessage(msg)
            MessageActionId.Edit -> onEdit(msg)
            MessageActionId.ReplyPrivately -> replyPrivately(dc, msg)
            MessageActionId.Save -> toggleSave(msg, save = true)
            MessageActionId.Unsave -> toggleSave(msg, save = false)
            MessageActionId.ExportAttachment ->
                DcHelper.openForViewOrShare(context, msgId, Intent.ACTION_VIEW)
            MessageActionId.Info -> showInfo(dc, msgId)
            MessageActionId.Resend -> dc.resendMsgs(intArrayOf(msgId))
            MessageActionId.AddToHomeScreen -> {
                val activity = context as? Activity
                if (activity != null) {
                    WebxdcActivity.addToHomeScreen(activity, msgId)
                }
            }
            MessageActionId.Delete -> {
                dc.deleteMsgs(intArrayOf(msgId))
                onDeleted()
            }
        }
    }

    private fun copyMessage(msg: DcMsg) {
        val text = msg.text?.trim().orEmpty()
        if (text.isEmpty()) return
        Util.writeTextToClipboard(context, text)
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun forwardMessage(msg: DcMsg) {
        val activity = context as? Activity ?: return
        try {
            val intent = Intent()
            setForwardingMessageIds(
                intent,
                intArrayOf(msg.id),
                DcHelper.getRpc(context).selectedAccountId,
            )
            HomeRelayingActivity.start(activity, intent)
        } catch (_: RpcException) {
            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun replyPrivately(dc: DcContext, msg: DcMsg) {
        val privateChatId = dc.createChatByContactId(msg.fromId)
        val replyMsg = DcMsg(dc, DcMsg.DC_MSG_TEXT)
        replyMsg.setQuote(msg)
        dc.setDraft(privateChatId, replyMsg)
        AppNav.openChatWithExtras(context, privateChatId)
    }

    private fun toggleSave(msg: DcMsg, save: Boolean) {
        try {
            val rpc = DcHelper.getRpc(context)
            if (save) {
                rpc.saveMsgs(rpc.selectedAccountId, Collections.singletonList(msg.id))
            } else {
                rpc.deleteMessages(rpc.selectedAccountId, Collections.singletonList(msg.savedMsgId))
            }
        } catch (_: RpcException) {
            Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInfo(dc: DcContext, msgId: Int) {
        val activity = context as? Activity ?: return
        val view = LayoutInflater.from(context).inflate(R.layout.message_details_view, null)
        view.findViewById<TextView>(R.id.details_text).text = dc.getMsgInfo(msgId)
        AlertDialog.Builder(activity)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
