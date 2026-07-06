package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import com.polli.domain.repository.AccountRepository
import com.polli.domain.repository.ChatRepository
import com.polli.domain.repository.MediaRepository
import com.polli.domain.repository.MessageRepository
import kotlin.jvm.JvmStatic

/**
 * Shared engine facade — platform code binds [Rpc] + account id after core startup.
 * Android: in-process FFI ([com.b44t.messenger.FFITransport]). Desktop: stdio subprocess.
 */
object PolliEngine {
    @Volatile
    private var session: Session? = null

    data class Session(
        val rpc: Rpc,
        val accountId: Int,
        val eventLoop: RpcEventLoop,
        val chat: ChatRepository,
        val messages: MessageRepository,
        val media: MediaRepository,
        val accounts: AccountRepository,
    )

    @JvmStatic
    fun bind(rpc: Rpc, accountId: Int, eventLoop: RpcEventLoop): Session {
        val accounts = RpcAccountRepository(rpc, accountId)
        val session =
            Session(
                rpc = rpc,
                accountId = accountId,
                eventLoop = eventLoop,
                chat = RpcChatRepository(rpc, accountId, eventLoop),
                messages = RpcMessageRepository(rpc, accountId, eventLoop),
                media = RpcMediaRepository(rpc, accountId),
                accounts = accounts,
            )
        this.session = session
        return session
    }

    @JvmStatic
    fun rebindAccount(accountId: Int) {
        val current = session ?: return
        (current.accounts as? RpcAccountRepository)?.updateAccountId(accountId)
        bind(current.rpc, accountId, current.eventLoop)
    }

    @JvmStatic
    fun get(): Session = session ?: error("PolliEngine not bound — call bind() after core init")

    @JvmStatic
    fun getOrNull(): Session? = session
}
