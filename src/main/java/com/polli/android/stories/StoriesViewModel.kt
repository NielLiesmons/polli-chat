package com.polli.android.stories

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.b44t.messenger.DcMsg
import com.polli.android.navigation.AppNav
import kotlinx.coroutines.launch
import org.thoughtcrime.securesms.connect.DcEventCenter
import org.thoughtcrime.securesms.connect.DcHelper

class StoriesViewModel(application: Application) : AndroidViewModel(application), DcEventCenter.DcEventDelegate {

    var posts by mutableStateOf<List<DcMsg>>(emptyList())
        private set

    private var chatId = -1
    private var registered = false

    fun bind(chatId: Int) {
        if (this.chatId == chatId && registered) {
            reload()
            return
        }
        this.chatId = chatId
        registerEvents()
        reload()
    }

    fun reload() {
        if (chatId <= 0) return
        val dc = DcHelper.getContext(getApplication())
        posts = AppNav.loadChannelPosts(dc, chatId)
    }

    private fun registerEvents() {
        if (registered) return
        val center = DcHelper.getEventCenter(getApplication())
        center.addMultiAccountObserver(DcContext.DC_EVENT_INCOMING_MSG, this)
        center.addObserver(DcContext.DC_EVENT_MSGS_CHANGED, this)
        center.addObserver(DcContext.DC_EVENT_MSG_DELETED, this)
        center.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this)
        registered = true
    }

    private fun unregisterEvents() {
        if (!registered) return
        DcHelper.getEventCenter(getApplication()).removeObservers(this)
        registered = false
    }

    override fun handleEvent(event: DcEvent) {
        if (event.id == DcContext.DC_EVENT_INCOMING_MSG || event.id == DcContext.DC_EVENT_MSGS_CHANGED) {
            val msg = DcHelper.getContext(getApplication()).getMsg(event.data1Int)
            if (msg.isOk && msg.chatId == chatId) {
                viewModelScope.launch { reload() }
            }
        } else if (event.id == DcContext.DC_EVENT_CHAT_MODIFIED && event.data1Int == chatId) {
            viewModelScope.launch { reload() }
        } else if (event.id == DcContext.DC_EVENT_MSG_DELETED) {
            viewModelScope.launch { reload() }
        }
    }

    override fun onCleared() {
        unregisterEvents()
        super.onCleared()
    }
}
