package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException
import com.polli.domain.repository.AccountRepository

class RpcAccountRepository(
    private val rpc: Rpc,
    private var accountId: Int,
) : AccountRepository {
    override val selectedAccountId: Int
        get() = accountId

    override val isConfigured: Boolean
        get() =
            try {
                rpc.isConfigured(accountId) == true
            } catch (_: RpcException) {
                false
            }

    fun updateAccountId(id: Int) {
        accountId = id
        try {
            rpc.selectAccount(id)
        } catch (_: RpcException) {
            // ignore
        }
    }
}
