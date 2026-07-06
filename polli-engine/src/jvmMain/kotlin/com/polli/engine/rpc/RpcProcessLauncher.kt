package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import chat.delta.rpc.StdioRpcTransport
import java.io.File

object RpcProcessLauncher {
    private const val SERVER_CMD = "deltachat-rpc-server"

    fun defaultAccountsPath(): File {
        val home = System.getProperty("user.home") ?: "."
        return File(home, ".polli/accounts").also { it.mkdirs() }
    }

    /** Returns null when the rpc-server binary is not on PATH. */
    fun start(accountsPath: File = defaultAccountsPath()): RpcSession? {
        if (!isServerAvailable()) return null
        val process =
            ProcessBuilder(SERVER_CMD)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .apply {
                    environment()["DC_ACCOUNTS_PATH"] = accountsPath.absolutePath
                }
                .start()
        val transport = StdioRpcTransport(process)
        return RpcSession(Rpc(transport), transport, process, accountsPath)
    }

    fun isServerAvailable(): Boolean =
        try {
            ProcessBuilder(SERVER_CMD, "--version")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
                .waitFor() == 0
        } catch (_: Exception) {
            false
        }
}

data class RpcSession(
    val rpc: chat.delta.rpc.Rpc,
    val transport: StdioRpcTransport,
    val process: Process,
    val accountsPath: File,
) : AutoCloseable {
    override fun close() {
        process.destroy()
        try {
            process.waitFor()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
