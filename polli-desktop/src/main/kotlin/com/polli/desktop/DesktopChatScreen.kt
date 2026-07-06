package com.polli.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.polli.domain.model.chat.ChatSessionInfo
import com.polli.domain.repository.MessageRepository
import com.polli.engine.rpc.PolliEngine
import com.polli.ui.chat.ChatController
import com.polli.ui.screens.ChatScreen

@Composable
fun DesktopChatScreen(
    engine: DesktopEngine,
    chatId: Int,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val messages: MessageRepository =
        remember(engine, engine.usingMock) {
            engine.messageRepository()
        }
    val session: ChatSessionInfo? =
        remember(chatId, engine.usingMock) {
            engine.chatSession(chatId)
        }
    val controller =
        remember(chatId, messages) {
            ChatController(messages = messages, scope = scope)
        }

    DisposableEffect(chatId) {
        controller.bind(chatId = chatId)
        onDispose { controller.dispose() }
    }

    ChatScreen(
        controller = controller,
        title = session?.name ?: "Chat #$chatId",
        onBack = onBack,
    )
}
