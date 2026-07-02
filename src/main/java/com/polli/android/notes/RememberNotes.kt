package com.polli.android.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import org.thoughtcrime.securesms.connect.DcEventCenter
import org.thoughtcrime.securesms.connect.DcHelper

@Composable
fun rememberNotes(): List<Note> {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(emptyList<Note>()) }
    var refreshTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun reload() {
        val dc = DcHelper.getContext(context)
        notes = NotesStore(dc).loadNotes()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) reload()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(refreshTick) {
        val center = DcHelper.getEventCenter(context)
        val delegate = DcEventCenter.DcEventDelegate { event ->
            when (event.id) {
                DcContext.DC_EVENT_MSGS_CHANGED,
                DcContext.DC_EVENT_INCOMING_MSG,
                DcContext.DC_EVENT_MSG_DELIVERED,
                DcContext.DC_EVENT_MSG_READ,
                -> refreshTick++
            }
        }
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSGS_CHANGED, delegate)
        center.addMultiAccountObserver(DcContext.DC_EVENT_INCOMING_MSG, delegate)
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSG_DELIVERED, delegate)
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSG_READ, delegate)
        reload()
        onDispose { center.removeObservers(delegate) }
    }

    return notes
}
