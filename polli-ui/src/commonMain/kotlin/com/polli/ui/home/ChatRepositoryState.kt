package com.polli.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.polli.domain.model.ArchiveLinkState
import com.polli.domain.model.InboxItem
import com.polli.domain.repository.ChatRepository

@Composable
fun rememberInboxItems(
    chatRepository: ChatRepository,
    searchQuery: String = "",
): List<InboxItem> {
    val q = searchQuery.trim().ifBlank { null }
    var items by remember(chatRepository) { mutableStateOf(chatRepository.loadInbox(q)) }

    LaunchedEffect(searchQuery) {
        items = chatRepository.loadInbox(q)
    }

    DisposableEffect(chatRepository, searchQuery) {
        val inboxObserver =
            chatRepository.observeInbox {
                items = chatRepository.loadInbox(q)
            }
        onDispose { inboxObserver.close() }
    }

    return items
}

@Composable
fun rememberArchiveLinkState(chatRepository: ChatRepository): ArchiveLinkState {
    var state by remember(chatRepository) { mutableStateOf(chatRepository.archiveLinkState()) }

    DisposableEffect(chatRepository) {
        val inboxObserver =
            chatRepository.observeInbox {
                state = chatRepository.archiveLinkState()
            }
        onDispose { inboxObserver.close() }
    }

    return state
}
