package com.polli.android.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.polli.android.data.engine.PolliRepositories
import com.polli.domain.model.ArchiveLinkState
import com.polli.domain.model.InboxItem
import com.polli.domain.repository.ChatRepository

@Composable
fun rememberArchiveLinkState(): ArchiveLinkState {
    val context = LocalContext.current
    val chatRepo = remember { PolliRepositories.chat(context) }
    var state by remember { mutableStateOf(chatRepo.archiveLinkState()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, chatRepo) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state = chatRepo.archiveLinkState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state
}

fun loadArchived(context: android.content.Context): List<InboxItem> =
    PolliRepositories.chat(context).loadArchived()

@Composable
fun rememberArchivedItems(): List<InboxItem> {
    val context = LocalContext.current
    val chatRepo = remember { PolliRepositories.chat(context) }
    var items by remember { mutableStateOf(chatRepo.loadArchived()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, chatRepo) {
        val inboxObserver = chatRepo.observeInbox { items = chatRepo.loadArchived() }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                items = chatRepo.loadArchived()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            inboxObserver.close()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return items
}
