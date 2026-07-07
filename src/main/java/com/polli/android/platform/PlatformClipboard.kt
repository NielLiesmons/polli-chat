package com.polli.android.platform

import android.content.Context
import com.polli.android.util.Util

object PlatformClipboard {
    fun copyText(context: Context, text: String) {
        Util.writeTextToClipboard(context, text)
    }
}
