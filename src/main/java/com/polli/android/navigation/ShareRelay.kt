package com.polli.android.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.polli.android.HomeActivity
import com.polli.android.HomeRelayingActivity
import com.polli.android.platform.PlatformShare
import org.thoughtcrime.securesms.R

/** Share / forward flows that pick a destination chat before opening the composer. */
object ShareRelay {

    fun isActive(context: Context): Boolean =
        context is Activity && PlatformShare.isRelayingMessageContent(context)

    fun relayTitle(context: Context): String? {
        val activity = context as? Activity ?: return null
        if (!PlatformShare.isRelayingMessageContent(activity)) return null
        return context.getString(
            if (PlatformShare.isSharing(activity)) {
                R.string.chat_share_with_title
            } else {
                R.string.forward_to
            },
        )
    }

    @JvmStatic
    @JvmOverloads
    fun openChat(context: Context, chatId: Int, startingPosition: Int = -1) {
        val intent = AppNav.chatIntent(context, chatId, startingPosition = startingPosition)
        if (context is Activity && PlatformShare.isRelayingMessageContent(context)) {
            PlatformShare.acquireRelayMessageContent(context, intent)
        }
        context.startActivity(intent)
        if (context is Activity && AppNav.useLabUi()) {
            context.overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out)
        }
        finishRelayShell(context)
    }

    @JvmStatic
    fun finishRelayShell(context: Context) {
        HomeRelayingActivity.finishActivity()
        if (context is Activity && context !is HomeActivity) {
            context.finish()
        }
    }
}
