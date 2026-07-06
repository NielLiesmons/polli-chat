package com.polli.ui.bridge

/**
 * Future cross-screen media chrome (voice, music, video) — mini-player overlay above tabs.
 * Not wired yet; chat bubbles use [com.polli.ui.components.audio.VoiceMessageBubble] locally.
 */
expect object PlatformMediaPlayback {
    val activeTitle: String?
    val isPlaying: Boolean

    fun play(uri: String, title: String? = null)

    fun pause()

    fun seek(fraction: Float)
}
