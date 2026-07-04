package com.polli.android.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.polli.android.data.engine.PolliRepositories
import com.polli.core.chat.ChatCategory
import com.polli.domain.model.InboxItem

/**
 * Loads inbox rows via [com.polli.domain.repository.ChatRepository] — same DC path as legacy list.
 */
@Composable
fun rememberInboxItems(searchQuery: String = ""): List<InboxItem> {
    val context = LocalContext.current
    val chatRepo = remember { PolliRepositories.chat(context) }
    val q = searchQuery.trim().ifBlank { null }
    var items by remember { mutableStateOf(chatRepo.loadInbox(q)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(searchQuery) {
        items = chatRepo.loadInbox(q)
    }

    DisposableEffect(lifecycleOwner, searchQuery, chatRepo) {
        val inboxObserver = chatRepo.observeInbox { items = chatRepo.loadInbox(q) }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                items = chatRepo.loadInbox(q)
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

@Composable
fun rememberChannels(items: List<InboxItem>): List<InboxItem> {
    return remember(items) { items.filter { it.category == ChatCategory.Channel } }
}
