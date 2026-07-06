package com.polli.ui.bridge

actual object PlatformMediaPlayback {
    actual val activeTitle: String? = null
    actual val isPlaying: Boolean = false

    actual fun play(uri: String, title: String?) {}

    actual fun pause() {}

    actual fun seek(fraction: Float) {}
}
