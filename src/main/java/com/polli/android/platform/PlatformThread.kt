package com.polli.android.platform

import org.thoughtcrime.securesms.util.Util

object PlatformThread {
    fun runOnMain(block: () -> Unit) {
        Util.runOnMain(block)
    }
}
