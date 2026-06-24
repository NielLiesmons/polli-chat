package com.polli.android.chat

import android.content.Context
import android.net.Uri
import android.util.Pair
import org.thoughtcrime.securesms.audio.AudioRecorder
import org.thoughtcrime.securesms.util.Util
import java.util.concurrent.ExecutionException

class VoiceRecorderBridge(context: Context) {
    private val recorder = AudioRecorder(context.applicationContext)
    private var active = false

    fun start() {
        if (active) return
        active = true
        recorder.startRecording()
    }

    fun stop(onResult: (Uri?, Long) -> Unit) {
        if (!active) {
            onResult(null, 0L)
            return
        }
        active = false
        val future = recorder.stopRecording()
        future.addListener(
            object : chat.delta.util.ListenableFuture.Listener<Pair<Uri, Long>> {
                override fun onSuccess(result: Pair<Uri, Long>) {
                    Util.runOnMain { onResult(result.first, result.second) }
                }

                override fun onFailure(e: ExecutionException) {
                    Util.runOnMain { onResult(null, 0L) }
                }
            },
        )
    }

    fun cancel() {
        if (!active) return
        active = false
        recorder.stopRecording()
    }
}
