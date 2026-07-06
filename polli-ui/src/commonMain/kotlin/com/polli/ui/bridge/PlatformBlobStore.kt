package com.polli.ui.bridge

expect object PlatformBlobStore {
    fun copyToBlobDir(sourcePath: String, prefix: String, suffix: String?): String?
}
