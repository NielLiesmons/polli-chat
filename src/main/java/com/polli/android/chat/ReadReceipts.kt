package com.polli.android.chat

import android.content.Context
import chat.delta.rpc.RpcException
import com.polli.android.data.engine.PolliRepositories
import com.polli.android.platform.EngineBridge

data class ReadReceiptUser(
    val contactId: Int,
    val name: String,
)

object ReadReceipts {
    fun load(context: Context, msgId: Int): List<ReadReceiptUser> {
        if (msgId <= 0) return emptyList()
        return try {
            val rpc = EngineBridge.getRpc(context)
            val accountId = PolliRepositories.accounts(context).selectedAccountId
            val dc = EngineBridge.getContext(context)
            rpc.getMessageReadReceipts(accountId, msgId)
                ?.mapNotNull { receipt ->
                    val id = receipt.contactId ?: return@mapNotNull null
                    val contact = dc.getContact(id)
                    ReadReceiptUser(
                        contactId = id,
                        name = contact.displayName?.takeIf { it.isNotBlank() } ?: "Unknown",
                    )
                }
                .orEmpty()
        } catch (_: RpcException) {
            emptyList()
        }
    }
}
