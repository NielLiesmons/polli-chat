package com.polli.ui.bridge

actual object PlatformBlobStore {
    actual fun copyToBlobDir(sourcePath: String, prefix: String, suffix: String?): String? =
        sourcePath
}
