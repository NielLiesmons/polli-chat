package com.polli.android.engine

import chat.delta.rpc.Rpc
import com.polli.engine.rpc.PolliEngine
import com.polli.engine.rpc.RpcEventLoop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Binds [PolliEngine] after [com.polli.android.ApplicationContext] creates Rpc. */
object PolliEngineHost {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var eventLoop: RpcEventLoop? = null

    @JvmStatic
    fun bindEngine(rpc: Rpc, accountId: Int) {
        val loop =
            eventLoop
                ?: RpcEventLoop(rpc, scope).also {
                    it.start()
                    eventLoop = it
                }
        PolliEngine.bind(rpc, accountId, loop)
    }

    @JvmStatic
    fun onAccountSwitch(accountId: Int) {
        com.polli.android.data.engine.PolliRepositories.resetMessageRepository()
        PolliEngine.rebindAccount(accountId)
    }
}
