package com.polli.android.platform

import android.content.Context
import android.content.Intent

/** Opens or shares a message attachment via the legacy content-provider bridge. */
object AttachmentIntents {
    fun openForView(context: Context, messageId: Int) {
        EngineBridge.openForViewOrShare(context, messageId, Intent.ACTION_VIEW)
    }

    fun share(context: Context, messageId: Int) {
        EngineBridge.openForViewOrShare(context, messageId, Intent.ACTION_SEND)
    }
}
