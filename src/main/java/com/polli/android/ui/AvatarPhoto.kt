package com.polli.android.ui

import android.content.Context
import com.b44t.messenger.DcContext
import com.polli.android.platform.EngineBridge
import java.io.File

/** Delta Chat's downloaded profile image on disk for this chat/contact, if any. */
object AvatarPhoto {
    /** Absolute path to the stored profile image, or null when none exists on disk. */
    fun storedImagePath(
        context: Context,
        chatId: Int?,
        contactId: Int?,
        dcContext: DcContext?,
    ): String? {
        try {
            val dc = dcContext ?: EngineBridge.getContext(context)
            if (contactId != null && contactId != 0) {
                return validPathOrNull(dc.getContact(contactId).profileImage)
            }
            if (chatId != null && chatId > 0) {
                return validPathOrNull(dc.getChat(chatId).profileImage)
            }
        } catch (_: Exception) {
        }
        return null
    }

    fun hasStoredImage(
        context: Context,
        chatId: Int?,
        contactId: Int?,
        dcContext: DcContext?,
    ): Boolean = storedImagePath(context, chatId, contactId, dcContext) != null

    private fun validPathOrNull(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return if (File(path).exists()) path else null
    }
}
