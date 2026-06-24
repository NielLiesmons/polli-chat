package com.polli.android.chat

import androidx.fragment.app.FragmentActivity
import org.thoughtcrime.securesms.components.AttachmentTypeSelector

object ChatAttachmentBridge {
    @JvmStatic
    fun showAttachmentMenu(activity: FragmentActivity, chatId: Int, anchor: android.view.View) {
        val selector = AttachmentTypeSelector(
            activity,
            activity.supportLoaderManager,
            null,
            chatId,
        )
        selector.show(activity, anchor)
    }
}
