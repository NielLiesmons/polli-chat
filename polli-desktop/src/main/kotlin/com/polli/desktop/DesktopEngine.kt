package com.polli.desktop

import chat.delta.rpc.RpcException
import chat.delta.rpc.types.EnteredLoginParam
import com.polli.domain.repository.ChatRepository
import com.polli.engine.rpc.RpcAccountResolver
import com.polli.engine.rpc.RpcChatRepository
import com.polli.engine.rpc.RpcEventLoop
import com.polli.engine.rpc.RpcProcessLauncher
import com.polli.engine.rpc.RpcQrHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class DesktopEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var session = RpcProcessLauncher.start()
    private var eventLoop: RpcEventLoop? = null
    private var accountId: Int = 0

    /** Set when import QR is a provider code — finish setup on AccountSetupScreen. */
    var pendingProviderQr: String? = null
        private set

    var usingMock: Boolean = false
        private set

    val canUseEngine: Boolean get() = session != null

    val statusMessage: String
        get() =
            when {
                session == null ->
                    "deltachat-rpc-server not found on PATH — install from chatmail/core releases."
                else -> "Accounts folder: ${session!!.accountsPath.absolutePath}"
            }

    var repository: ChatRepository = buildRepository()
        private set

    val needsOnboarding: Boolean
        get() = !usingMock && !isConfigured()

    fun initialDisplayName(): String =
        try {
            session?.rpc?.getConfig(accountId, CONFIG_DISPLAY_NAME)?.orEmpty().orEmpty()
        } catch (_: RpcException) {
            ""
        }

    fun profileSeed(): String =
        try {
            session?.rpc?.getConfig(accountId, CONFIG_CONFIGURED_ADDRESS)?.orEmpty().orEmpty()
        } catch (_: RpcException) {
            ""
        }

    private fun buildRepository(): ChatRepository {
        val active = session
        if (usingMock || active == null) {
            return MockChatRepository()
        }
        if (accountId <= 0 || active.rpc.isConfigured(accountId) != true) {
            accountId = RpcAccountResolver.resolve(active.rpc, accountId)
        }
        try {
            if (active.rpc.isConfigured(accountId) == true) {
                active.rpc.startIo(accountId)
            }
        } catch (_: RpcException) {
        }
        val loop = RpcEventLoop(active.rpc, scope).also { it.start() }
        eventLoop?.close()
        eventLoop = loop
        val repo = RpcChatRepository(active.rpc, accountId, loop)
        val inbox = repo.loadInbox()
        println(
            "[DesktopEngine] accountId=$accountId inbox=${inbox.size} " +
                inbox.groupBy { it.category }.mapValues { it.value.size },
        )
        return repo
    }

    private fun isConfigured(): Boolean {
        val active = session ?: return false
        return RpcAccountResolver.hasConfiguredAccount(active.rpc)
    }

    fun createAccountWithDisplayName(
        displayName: String,
        providerQr: String = pendingProviderQr ?: DEFAULT_PROVIDER_QR,
    ): Result<Unit> {
        val active = session ?: return Result.failure(IllegalStateException("Engine unavailable"))
        return try {
            if (accountId == 0) accountId = active.rpc.addAccount()
            if (displayName.isNotBlank()) {
                active.rpc.setConfig(accountId, CONFIG_DISPLAY_NAME, displayName)
            }
            when (val outcome = RpcQrHandler.importProviderQr(active.rpc, accountId, providerQr)) {
                is RpcQrHandler.Outcome.Success -> {
                    accountId = outcome.accountId
                    pendingProviderQr = null
                    repository = buildRepository()
                    Result.success(Unit)
                }
                is RpcQrHandler.Outcome.Error ->
                    Result.failure(IllegalStateException(outcome.message))
                is RpcQrHandler.Outcome.NeedsDisplayName ->
                    Result.failure(IllegalStateException("Unexpected QR state"))
            }
        } catch (e: RpcException) {
            Result.failure(e)
        }
    }

    sealed class QrHandleResult {
        data object Done : QrHandleResult()

        data class NeedDisplayName(val providerQr: String) : QrHandleResult()

        data class Failed(val message: String) : QrHandleResult()
    }

    fun handleQr(qrContent: String, linkSecondDevice: Boolean): QrHandleResult {
        val active = session ?: return QrHandleResult.Failed(statusMessage)
        if (accountId == 0) accountId = RpcAccountResolver.resolve(active.rpc)
        return when (
            val outcome =
                RpcQrHandler.handle(active.rpc, accountId, qrContent, linkSecondDevice)
        ) {
            is RpcQrHandler.Outcome.Success -> {
                accountId = outcome.accountId
                pendingProviderQr = null
                repository = buildRepository()
                QrHandleResult.Done
            }
            is RpcQrHandler.Outcome.NeedsDisplayName -> {
                pendingProviderQr = outcome.providerQr
                QrHandleResult.NeedDisplayName(outcome.providerQr)
            }
            is RpcQrHandler.Outcome.Error -> QrHandleResult.Failed(outcome.message)
        }
    }

    fun configureManualLogin(email: String, password: String): Result<Unit> {
        val active = session ?: return Result.failure(IllegalStateException("Engine unavailable"))
        return try {
            if (accountId == 0) accountId = active.rpc.addAccount()
            val param = EnteredLoginParam()
            param.addr = email
            param.password = password
            active.rpc.addOrUpdateTransport(accountId, param)
            active.rpc.startIo(accountId)
            repository = buildRepository()
            Result.success(Unit)
        } catch (e: RpcException) {
            Result.failure(e)
        }
    }

    fun refreshAfterSetup() {
        repository = buildRepository()
    }

    fun useMock() {
        usingMock = true
        repository = MockChatRepository()
    }

    fun close() {
        eventLoop?.close()
        session?.close()
    }

    companion object {
        const val CONFIG_DISPLAY_NAME = "displayname"
        const val CONFIG_CONFIGURED_ADDRESS = "configured_addr"
        const val DEFAULT_PROVIDER_QR = "dcaccount:nine.testrun.org"
    }
}
