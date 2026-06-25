package com.polli.android.media

import android.content.Context
import com.b44t.messenger.DcMsg
import org.thoughtcrime.securesms.connect.DcHelper

data class MediaGalleryPage(
    val messageIds: IntArray,
    val initialIndex: Int,
)

object MediaGalleryLoad {
    fun galleryForMessage(context: Context, msgId: Int, leftIsRecent: Boolean = true): MediaGalleryPage? {
        val dc = DcHelper.getContext(context)
        val msg = dc.getMsg(msgId)
        if (!msg.isOk) return null
        val raw = dc.getChatMedia(
            msg.chatId,
            DcMsg.DC_MSG_IMAGE,
            DcMsg.DC_MSG_GIF,
            DcMsg.DC_MSG_VIDEO,
        ) ?: return null
        if (raw.isEmpty()) return null
        val mediaMessages = if (leftIsRecent) raw else raw.reversedArray()
        var currentIndex = mediaMessages.indexOf(msgId)
        if (currentIndex < 0) currentIndex = 0
        return MediaGalleryPage(mediaMessages, currentIndex)
    }

    fun mediaIds(
        context: Context,
        chatId: Int,
        type1: Int,
        type2: Int = 0,
        type3: Int = 0,
    ): IntArray {
        val dc = DcHelper.getContext(context)
        return dc.getChatMedia(chatId, type1, type2, type3) ?: intArrayOf()
    }
}
