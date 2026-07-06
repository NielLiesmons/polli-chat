package com.polli.android.platform

import android.content.Context
import android.graphics.Bitmap
import org.thoughtcrime.securesms.profiles.AvatarHelper
import java.io.File

object PlatformAvatars {
    const val AVATAR_SIZE: Int = AvatarHelper.AVATAR_SIZE

    fun getSelfAvatarFile(context: Context): File = AvatarHelper.getSelfAvatarFile(context)

    fun setSelfAvatar(context: Context, bitmap: Bitmap?) {
        AvatarHelper.setSelfAvatar(context, bitmap)
    }

    fun setGroupAvatar(context: Context, chatId: Int, bitmap: Bitmap?) {
        AvatarHelper.setGroupAvatar(context, chatId, bitmap)
    }
}
