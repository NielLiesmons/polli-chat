package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode

/**
 * Reads `check_qr` results without deserializing into generated [chat.delta.rpc.types.Qr].
 *
 * The vendored Java RPC types expect PascalCase `kind` tags (`Backup2`), but current
 * chatmail RPC servers emit camelCase (`backup2`). Android avoids this via JNI [DcLot].
 */
internal object RpcQrKinds {
    fun checkKind(rpc: Rpc, accountId: Int, qrText: String): String {
        val mapper = rpc.transport.getObjectMapper()
        val node: JsonNode =
            rpc.transport.callForResult(
                object : TypeReference<JsonNode>() {},
                "check_qr",
                mapper.valueToTree(accountId),
                mapper.valueToTree(qrText),
            )
        return node.path("kind").asText("")
    }
}
