package com.polli.android.chat

import android.app.Activity
import android.net.Uri
import com.polli.android.data.engine.PolliRepositories
import org.thoughtcrime.securesms.util.SendRelayedMessageUtil
import org.thoughtcrime.securesms.util.ShareUtil

/** Applies share / forward intent payloads when opening [ChatActivity]. */
object ShareInbound {

    fun apply(
        activity: Activity,
        chatId: Int,
        viewModel: ChatViewModel,
        stageAttachment: (Uri, String?) -> Unit,
    ) {
        when {
            ShareUtil.isForwarding(activity) -> handleForward(activity, chatId)
            ShareUtil.isSharing(activity) -> handleShare(activity, chatId, viewModel, stageAttachment)
        }
    }

    private fun handleForward(activity: Activity, chatId: Int) {
        val session = PolliRepositories.chat(activity).getSession(chatId)
        if (session?.isSelfTalk == true) {
            SendRelayedMessageUtil.immediatelyRelay(activity, chatId)
            return
        }
        val messageIds = ShareUtil.getForwardedMessageIDs(activity) ?: intArrayOf()
        val count = messageIds.size
        val chatName = session?.name ?: "Chat"
        android.app.AlertDialog.Builder(activity)
            .setMessage(
                activity.resources.getQuantityString(
                    org.thoughtcrime.securesms.R.plurals.ask_forward_messages,
                    count,
                    count,
                    chatName,
                ),
            )
            .setPositiveButton(org.thoughtcrime.securesms.R.string.forward) { _, _ ->
                SendRelayedMessageUtil.immediatelyRelay(activity, chatId)
            }
            .setNegativeButton(org.thoughtcrime.securesms.R.string.cancel) { _, _ -> activity.finish() }
            .setOnCancelListener { activity.finish() }
            .show()
    }

    private fun handleShare(
        activity: Activity,
        chatId: Int,
        viewModel: ChatViewModel,
        stageAttachment: (Uri, String?) -> Unit,
    ) {
        val uris = ShareUtil.getSharedUris(activity)
        val text = ShareUtil.getSharedText(activity)
        if (uris.size > 1) {
            SendRelayedMessageUtil.immediatelyRelay(activity, chatId)
            return
        }
        if (uris.isEmpty()) {
            text?.takeIf { it.isNotBlank() }?.let { viewModel.updateDraft(it) }
            return
        }
        val uri = uris[0]
        val mime = org.thoughtcrime.securesms.util.MediaUtil.getMimeType(activity, uri)
        stageAttachment(uri, mime)
        text?.takeIf { it.isNotBlank() }?.let { viewModel.updateDraft(it) }
    }
}
