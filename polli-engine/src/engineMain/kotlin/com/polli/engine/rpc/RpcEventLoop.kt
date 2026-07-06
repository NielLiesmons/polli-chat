package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/** Polls Chatmail events and notifies inbox listeners. */
class RpcEventLoop(
    private val rpc: Rpc,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private var job: Job? = null

    fun addListener(listener: () -> Unit): AutoCloseable {
        listeners.add(listener)
        return AutoCloseable { listeners.remove(listener) }
    }

    fun start() {
        if (job != null) return
        job =
            scope.launch(Dispatchers.IO) {
                while (isActive) {
                    try {
                        val batch = rpc.getNextEventBatch()
                        if (batch.isNotEmpty()) {
                            listeners.forEach { it.invoke() }
                        }
                    } catch (_: RpcException) {
                        // Server stopped or transport closed.
                        break
                    }
                }
            }
    }

    override fun close() {
        job?.cancel()
        job = null
        listeners.clear()
    }
}
