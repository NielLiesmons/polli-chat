package com.polli.android.data.engine

import android.content.Context
import com.polli.android.platform.EngineBridge
import com.polli.domain.repository.AccountRepository
import com.polli.domain.repository.ChatRepository
import com.polli.domain.repository.MediaRepository
import com.polli.domain.repository.MessageRepository
import com.polli.engine.rpc.PolliEngine
import com.polli.android.platform.PolliApplication

/** Android wiring — JNI reads for chat feed (DC parity), JSON-RPC for writes/desktop parity. */
object PolliRepositories {
    @Volatile
    var messagesOverride: MessageRepository? = null

    @Volatile
    private var jniMessages: JniMessageRepository? = null

    private fun session(context: Context) =
        PolliEngine.getOrNull()
            ?: run {
                PolliApplication.getInstance(context)
                PolliEngine.getOrNull()
                    ?: error("PolliEngine not bound yet")
            }

    fun chat(context: Context): ChatRepository = session(context).chat

    fun media(context: Context): MediaRepository = session(context).media

    fun messages(context: Context): MessageRepository {
        messagesOverride?.let { return it }
        jniMessages?.let { return it }
        val engine = session(context)
        return JniMessageRepository(
            dc = EngineBridge.getContext(context),
            writes = engine.messages,
        ).also { jniMessages = it }
    }

    /** Call on account switch so JNI cache does not cross accounts. */
    fun resetMessageRepository() {
        jniMessages = null
    }

    fun accounts(context: Context): AccountRepository = session(context).accounts
}
