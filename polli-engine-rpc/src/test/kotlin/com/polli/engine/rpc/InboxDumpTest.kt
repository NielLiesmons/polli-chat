package com.polli.engine.rpc

import chat.delta.rpc.RpcException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.polli.core.chat.ChatListFlags
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue

/** Local diagnostic — run with `./gradlew :polli-engine-rpc:test --tests InboxDumpTest` */
class InboxDumpTest {
    @Test
    fun dumpInboxFromLiveRpc() {
        assumeTrue(RpcProcessLauncher.isServerAvailable(), "deltachat-rpc-server not on PATH")
        val session = RpcProcessLauncher.start() ?: error("failed to start rpc")
        session.use {
            val rpc = it.rpc
            val accountId = RpcAccountResolver.resolve(rpc, 0)
            println("=== Polli inbox dump ===")
            println("accountId=$accountId selected=${rpc.getSelectedAccountId()} configured=${rpc.isConfigured(accountId)}")
            try {
                rpc.startIo(accountId)
            } catch (e: RpcException) {
                println("startIo: ${e.message}")
            }
            val entries =
                rpc.getChatlistEntries(accountId, ChatListFlags.INBOX, null, null) ?: emptyList()
            println("getChatlistEntries count=${entries.size} ids=${entries.take(20)}")
            if (entries.isEmpty()) return

            val mapper = rpc.transport.getObjectMapper()
            val sample = entries.take(3)
            val raw: JsonNode =
                rpc.transport.callForResult(
                    object : TypeReference<JsonNode>() {},
                    "get_chatlist_items_by_entries",
                    mapper.valueToTree(accountId),
                    mapper.valueToTree(sample),
                )
            println("raw JSON keys: ${raw.fieldNames().asSequence().toList()}")
            println("raw sample:\n${raw.toPrettyString().take(6000)}")

            val mapped = RpcChatListMapper.load(rpc, accountId, ChatListFlags.INBOX, null)
            println("mapped count=${mapped.size}")
            mapped.groupBy { it.category }.forEach { (cat, rows) ->
                println("  $cat: ${rows.size}")
            }
            mapped.take(10).forEach { row ->
                println("  #${row.chatId} ${row.category} \"${row.name}\"")
            }
        }
    }
}
