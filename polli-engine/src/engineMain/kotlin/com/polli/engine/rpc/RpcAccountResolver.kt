package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException

object RpcAccountResolver {
    /** Prefer RPC selected account (accounts.toml), then an explicit preferred id, then configured accounts. */
    fun resolve(rpc: Rpc, preferred: Int = 0): Int {
        return try {
            val selected = rpc.getSelectedAccountId()
            if (selected != null && selected > 0 && rpc.isConfigured(selected) == true) {
                rpc.selectAccount(selected)
                return selected
            }
            if (preferred > 0 && rpc.isConfigured(preferred) == true) {
                rpc.selectAccount(preferred)
                return preferred
            }
            val ids = rpc.getAllAccountIds()
            if (ids.isNullOrEmpty()) {
                return rpc.addAccount()
            }
            val configured = ids.filter { rpc.isConfigured(it) == true }
            if (configured.isNotEmpty()) {
                val id = configured.max()
                rpc.selectAccount(id)
                return id
            }
            val fallback = ids.first()
            if (fallback > 0) {
                rpc.selectAccount(fallback)
            }
            fallback
        } catch (_: RpcException) {
            preferred.takeIf { it > 0 } ?: 0
        }
    }

    fun hasConfiguredAccount(rpc: Rpc): Boolean {
        return try {
            rpc.getAllAccountIds()?.any { rpc.isConfigured(it) == true } == true
        } catch (_: RpcException) {
            false
        }
    }
}
