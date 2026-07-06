package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException

/** Routes scanned/pasted QR codes to the correct Chatmail RPC — mirrors Android QrResultHandler. */
object RpcQrHandler {
    sealed class Outcome {
        data class Success(val accountId: Int) : Outcome()

        data class NeedsDisplayName(val providerQr: String) : Outcome()

        data class Error(val message: String) : Outcome()
    }

    fun handle(
        rpc: Rpc,
        accountId: Int,
        qrText: String,
        preferSecondDevice: Boolean = false,
    ): Outcome {
        val trimmed = qrText.trim()
        if (trimmed.isEmpty()) {
            return Outcome.Error("QR code is empty")
        }
        return try {
            var accId = accountId.takeIf { it > 0 } ?: rpc.addAccount()
            when {
                isBackupQr(trimmed) -> importBackup(rpc, accId, trimmed)
                isProviderQr(trimmed) ->
                    if (preferSecondDevice) {
                        Outcome.Error("This QR code is for a new account, not linking a second device")
                    } else {
                        Outcome.NeedsDisplayName(trimmed)
                    }
                else -> routeByKind(rpc, accId, trimmed, preferSecondDevice)
            }
        } catch (e: RpcException) {
            Outcome.Error(e.message ?: "QR import failed")
        }
    }

    fun importProviderQr(rpc: Rpc, accountId: Int, providerQr: String): Outcome {
        return try {
            var accId = accountId.takeIf { it > 0 } ?: rpc.addAccount()
            rpc.addTransportFromQr(accId, providerQr.trim())
            rpc.startIo(accId)
            Outcome.Success(accId)
        } catch (e: RpcException) {
            Outcome.Error(e.message ?: "Account setup failed")
        }
    }

    private fun routeByKind(
        rpc: Rpc,
        accountId: Int,
        trimmed: String,
        preferSecondDevice: Boolean,
    ): Outcome {
        val kind = RpcQrKinds.checkKind(rpc, accountId, trimmed).lowercase()
        return when (kind) {
            "backup2" -> importBackup(rpc, accountId, trimmed)
            "account", "login" ->
                if (preferSecondDevice) {
                    Outcome.Error("This QR code is for a new account, not linking a second device")
                } else {
                    Outcome.NeedsDisplayName(trimmed)
                }
            "backuptoonew" ->
                Outcome.Error("Backup format is too new — update Polli Desktop")
            else -> Outcome.Error("QR code cannot be used here")
        }
    }

    private fun importBackup(rpc: Rpc, accountId: Int, qrText: String): Outcome {
        var accId = accountForBackupImport(rpc, accountId)
        rpc.stopIoForAllAccounts()
        rpc.getBackup(accId, qrText)
        rpc.selectAccount(accId)
        rpc.startIo(accId)
        return Outcome.Success(accId)
    }

    private fun isBackupQr(raw: String): Boolean =
        raw.uppercase().startsWith("DCBACKUP2:")

    private fun isProviderQr(raw: String): Boolean {
        val lower = raw.lowercase()
        return lower.startsWith("dcaccount:") || lower.startsWith("dclogin:")
    }

    private fun accountForBackupImport(rpc: Rpc, accountId: Int): Int {
        return if (rpc.isConfigured(accountId) == true) {
            rpc.addAccount()
        } else {
            accountId
        }
    }
}
