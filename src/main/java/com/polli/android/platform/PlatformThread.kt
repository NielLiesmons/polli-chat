package com.polli.android.platform

import com.polli.android.util.Util

object PlatformThread {
    fun runOnMain(block: () -> Unit) {
        Util.runOnMain(block)
    }
}
