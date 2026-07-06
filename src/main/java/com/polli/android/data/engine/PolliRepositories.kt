package com.polli.android.data.engine

import android.content.Context
import com.polli.domain.repository.AccountRepository
import com.polli.domain.repository.ChatRepository
import com.polli.domain.repository.MediaRepository
import com.polli.domain.repository.MessageRepository
import com.polli.engine.rpc.PolliEngine
import com.polli.android.platform.PolliApplication

/** Android wiring for shared [PolliEngine] repositories (JSON-RPC over FFI). */
object PolliRepositories {
    private fun session(context: Context) =
        PolliEngine.getOrNull()
            ?: run {
                val app = PolliApplication.getInstance(context)
                PolliEngine.getOrNull()
                    ?: error("PolliEngine not bound yet")
            }

    fun chat(context: Context): ChatRepository = session(context).chat

    fun media(context: Context): MediaRepository = session(context).media

    fun messages(context: Context): MessageRepository = session(context).messages

    fun accounts(context: Context): AccountRepository = session(context).accounts
}
