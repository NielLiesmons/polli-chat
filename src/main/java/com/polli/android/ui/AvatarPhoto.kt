package com.polli.android.ui

import android.content.Context
import com.b44t.messenger.DcContext
import org.thoughtcrime.securesms.connect.DcHelper
import java.io.File

/** Whether Delta Chat has a downloaded profile image on disk for this chat/contact. */
object AvatarPhoto {
    fun hasStoredImage(
        context: Context,
        chatId: Int?,
        contactId: Int?,
        dcContext: DcContext?,
    ): Boolean {
        try {
            val dc = dcContext ?: DcHelper.getContext(context)
            if (contactId != null && contactId != 0) {
                return isValidPath(dc.getContact(contactId).profileImage)
            }
            if (chatId != null && chatId > 0) {
                return isValidPath(dc.getChat(chatId).profileImage)
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun isValidPath(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return File(path).exists()
    }
}
