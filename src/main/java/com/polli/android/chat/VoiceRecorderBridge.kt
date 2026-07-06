package com.polli.android.chat

import android.content.Context
import android.net.Uri
import android.util.Pair
import com.polli.android.platform.PolliVoiceRecorder
import com.polli.android.platform.PlatformThread
import java.util.concurrent.ExecutionException

class VoiceRecorderBridge(context: Context) {
    private val recorder = PolliVoiceRecorder(context.applicationContext)
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
                    PlatformThread.runOnMain { onResult(result.first, result.second) }
                }

                override fun onFailure(e: ExecutionException) {
                    PlatformThread.runOnMain { onResult(null, 0L) }
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
