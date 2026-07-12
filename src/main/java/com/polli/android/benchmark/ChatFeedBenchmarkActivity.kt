package com.polli.android.benchmark

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.polli.android.BaseComposeActivity
import com.polli.android.BuildConfig
import com.polli.android.chat.ChatScreen
import com.polli.android.chat.ChatViewModel
import com.polli.android.data.engine.PolliRepositories
import com.polli.android.platform.PolliAudioPlaybackViewModel
import com.polli.android.settings.AppPrefs
import com.polli.android.settings.LocalAppPrefs
import com.polli.android.theme.PolliTheme
import com.polli.domain.model.chat.ChatActionContext

/** Hosts the chat feed with a synthetic in-memory repository for perf benchmarks. */
class ChatFeedBenchmarkActivity : BaseComposeActivity() {
    private val viewModel: ChatViewModel by viewModels()
    private val playbackViewModel: PolliAudioPlaybackViewModel by viewModels()
    private val benchmarkRepo = BenchmarkMessageRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!BuildConfig.BENCHMARK_MODE) {
            finish()
            return
        }
        PolliRepositories.messagesOverride = benchmarkRepo
        super.onCreate(savedInstanceState)
        val chatId = BenchmarkMessageRepository.BENCHMARK_CHAT_ID
        viewModel.bind(chatId = chatId)
        val prefs = AppPrefs(this)
        val session =
            ChatActionContext(
                canSend = true,
                isEncrypted = true,
                isMultiUser = false,
                isSelfTalk = false,
            )
        setContent {
            PolliTheme(prefs = prefs) {
                CompositionLocalProvider(LocalAppPrefs provides prefs) {
                    ChatScreen(
                        viewModel = viewModel,
                        chatTitle = "Benchmark Chat",
                        chatSeed = "benchmark",
                        chatId = chatId,
                        chatSession = session,
                        isGroup = false,
                        isBroadcast = false,
                        playbackViewModel = playbackViewModel,
                        onBack = { finish() },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        PolliRepositories.messagesOverride = null
        super.onDestroy()
    }

    fun benchmarkRepository(): BenchmarkMessageRepository = benchmarkRepo
}
