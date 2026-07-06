package com.polli.android.platform

import android.content.Context
import android.content.Intent
import org.thoughtcrime.securesms.connect.DcHelper

/** Opens or shares a message attachment via the legacy content-provider bridge. */
object AttachmentIntents {
    fun openForView(context: Context, messageId: Int) {
        DcHelper.openForViewOrShare(context, messageId, Intent.ACTION_VIEW)
    }

    fun share(context: Context, messageId: Int) {
        DcHelper.openForViewOrShare(context, messageId, Intent.ACTION_SEND)
    }
}
