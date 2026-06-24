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
import com.polli.android.bridge.ChatListMapper
import com.polli.android.bridge.InboxItem
import com.polli.core.chat.ChatCategory

/**
 * Loads inbox rows directly from DC [DcChatlist] — same API path as ConversationListFragment.
 */
@Composable
fun rememberInboxItems(searchQuery: String = ""): List<InboxItem> {
    val context = LocalContext.current
    val q = searchQuery.trim().ifBlank { null }
    var items by remember { mutableStateOf(ChatListMapper.load(context, q)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(searchQuery) {
        items = ChatListMapper.load(context, q)
    }

    DisposableEffect(lifecycleOwner, searchQuery) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                items = ChatListMapper.load(context, q)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return items
}

@Composable
fun rememberChannels(items: List<InboxItem>): List<InboxItem> {
    return remember(items) { items.filter { it.category == ChatCategory.Channel } }
}
