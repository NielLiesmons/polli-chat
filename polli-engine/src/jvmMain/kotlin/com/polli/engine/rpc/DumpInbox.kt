package com.polli.engine.rpc

import com.polli.core.chat.ChatListFlags

fun main() {
    val session = RpcProcessLauncher.start() ?: error("deltachat-rpc-server not on PATH")
    session.use {
        val rpc = it.rpc
        val accountId = RpcAccountResolver.resolve(rpc, 0)
        rpc.selectAccount(accountId)
        try {
            rpc.startIo(accountId)
        } catch (_: Exception) {
        }
        val entries = rpc.getChatlistEntries(accountId, ChatListFlags.INBOX, null, null) ?: emptyList()
        println("entries=${entries.size} ids=$entries")
        val mapped = RpcChatListMapper.load(rpc, accountId, ChatListFlags.INBOX, null)
        println("mapped=${mapped.size}")
        mapped.groupBy { it.category }.forEach { (cat, rows) -> println("  $cat: ${rows.size}") }
        mapped.forEach { println("  #${it.chatId} ${it.category} ${it.name}") }
    }
}
