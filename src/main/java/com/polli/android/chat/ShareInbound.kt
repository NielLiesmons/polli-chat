package com.polli.android.chat

import android.app.Activity
import android.net.Uri
import com.polli.android.data.engine.PolliRepositories
import com.polli.android.platform.PlatformMedia
import com.polli.android.platform.PlatformShare
import com.polli.android.R

/** Applies share / forward intent payloads when opening [ChatActivity]. */
object ShareInbound {

    fun apply(
        activity: Activity,
        chatId: Int,
        viewModel: ChatViewModel,
        stageAttachment: (Uri, String?) -> Unit,
    ) {
        when {
            PlatformShare.isForwarding(activity) -> handleForward(activity, chatId)
            PlatformShare.isSharing(activity) -> handleShare(activity, chatId, viewModel, stageAttachment)
        }
    }

    private fun handleForward(activity: Activity, chatId: Int) {
        val session = PolliRepositories.chat(activity).getSession(chatId)
        if (session?.isSelfTalk == true) {
            PlatformShare.relayImmediately(activity, chatId)
            return
        }
        val messageIds = PlatformShare.getForwardedMessageIds(activity) ?: intArrayOf()
        val count = messageIds.size
        val chatName = session?.name ?: "Chat"
        android.app.AlertDialog.Builder(activity)
            .setMessage(
                activity.resources.getQuantityString(
                    R.plurals.ask_forward_messages,
                    count,
                    count,
                    chatName,
                ),
            )
            .setPositiveButton(R.string.forward) { _, _ ->
                PlatformShare.relayImmediately(activity, chatId)
            }
            .setNegativeButton(R.string.cancel) { _, _ -> activity.finish() }
            .setOnCancelListener { activity.finish() }
            .show()
    }

    private fun handleShare(
        activity: Activity,
        chatId: Int,
        viewModel: ChatViewModel,
        stageAttachment: (Uri, String?) -> Unit,
    ) {
        val uris = PlatformShare.getSharedUris(activity)
        val text = PlatformShare.getSharedText(activity)
        if (uris.size > 1) {
            PlatformShare.relayImmediately(activity, chatId)
            return
        }
        if (uris.isEmpty()) {
            text?.takeIf { it.isNotBlank() }?.let { viewModel.updateDraft(it) }
            return
        }
        val uri = uris[0]
        val mime = PlatformMedia.mimeType(activity, uri)
        stageAttachment(uri, mime)
        text?.takeIf { it.isNotBlank() }?.let { viewModel.updateDraft(it) }
    }
}
