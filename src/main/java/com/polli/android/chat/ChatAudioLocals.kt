package com.polli.android.chat

import androidx.compose.runtime.compositionLocalOf
import org.thoughtcrime.securesms.components.audioplay.AudioPlaybackViewModel

val LocalChatAudioPlayback = compositionLocalOf<AudioPlaybackViewModel?> { null }
