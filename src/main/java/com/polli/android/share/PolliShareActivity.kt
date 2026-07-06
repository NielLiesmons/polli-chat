package com.polli.android.share

import org.thoughtcrime.securesms.ShareActivity

/**
 * Polli share target for OS share sheet and direct-share shortcuts.
 * Inherits legacy URI resolution from [ShareActivity] until fully ported to Kotlin.
 */
class PolliShareActivity : ShareActivity() {
    companion object {
        const val EXTRA_ACC_ID: String = ShareActivity.EXTRA_ACC_ID
        const val EXTRA_CHAT_ID: String = ShareActivity.EXTRA_CHAT_ID
    }
}
