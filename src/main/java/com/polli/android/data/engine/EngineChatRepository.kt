package com.polli.android.data.engine

import android.content.Context
import com.b44t.messenger.DcChat
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.polli.android.bridge.ChatListMapper
import com.polli.domain.model.ArchiveLinkRules
import com.polli.domain.model.ArchiveLinkState
import com.polli.domain.model.InboxItem
import com.polli.domain.repository.ChatRepository
import org.thoughtcrime.securesms.connect.DcEventCenter
import org.thoughtcrime.securesms.connect.DcHelper

/** Android adapter — wraps the Chatmail engine JNI behind [ChatRepository]. */
class EngineChatRepository(context: Context) : ChatRepository {
    private val appContext = context.applicationContext

    override fun loadInbox(query: String?): List<InboxItem> =
        ChatListMapper.load(appContext, query)

    override fun loadArchived(query: String?): List<InboxItem> =
        ChatListMapper.loadArchived(appContext, query)

    override fun archiveLinkState(): ArchiveLinkState {
        val ctx = DcHelper.getContext(appContext)
        val chatlist = ctx.getChatlist(DcContext.DC_GCL_ARCHIVED_ONLY, null, 0)
        val cnt = chatlist.getCnt()
        val unread = if (cnt > 0) {
            ctx.getFreshMsgCount(DcChat.DC_CHAT_ID_ARCHIVED_LINK).coerceAtLeast(0)
        } else {
            0
        }
        return ArchiveLinkRules.linkState(cnt, unread)
    }

    override fun getFreshMessageCount(chatId: Int): Int =
        DcHelper.getContext(appContext).getFreshMsgCount(chatId).coerceAtLeast(0)

    override fun observeInbox(onUpdate: () -> Unit): AutoCloseable =
        InboxEventObserver(appContext, onUpdate)
}

private class InboxEventObserver(
    context: Context,
    private val onUpdate: () -> Unit,
) : DcEventCenter.DcEventDelegate, AutoCloseable {
    private val appContext = context.applicationContext
    private var registered = false

    init {
        val center = DcHelper.getEventCenter(appContext)
        center.addMultiAccountObserver(DcContext.DC_EVENT_INCOMING_MSG, this)
        center.addMultiAccountObserver(DcContext.DC_EVENT_MSGS_NOTICED, this)
        center.addMultiAccountObserver(DcContext.DC_EVENT_CHAT_DELETED, this)
        center.addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this)
        center.addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this)
        center.addObserver(DcContext.DC_EVENT_MSGS_CHANGED, this)
        center.addObserver(DcContext.DC_EVENT_MSG_DELIVERED, this)
        center.addObserver(DcContext.DC_EVENT_MSG_FAILED, this)
        center.addObserver(DcContext.DC_EVENT_MSG_READ, this)
        center.addObserver(DcContext.DC_EVENT_REACTIONS_CHANGED, this)
        center.addObserver(DcContext.DC_EVENT_CONNECTIVITY_CHANGED, this)
        center.addObserver(DcContext.DC_EVENT_SELFAVATAR_CHANGED, this)
        registered = true
    }

    override fun handleEvent(event: DcEvent) {
        val ctx = DcHelper.getContext(appContext)
        if (event.accountId != ctx.accountId && event.id != DcContext.DC_EVENT_CHAT_DELETED) {
            return
        }
        onUpdate()
    }

    override fun close() {
        if (!registered) return
        DcHelper.getEventCenter(appContext).removeObservers(this)
        registered = false
    }
}

object PolliRepositories {
    @Volatile
    private var chat: EngineChatRepository? = null
    @Volatile
    private var media: EngineMediaRepository? = null

    fun chat(context: Context): ChatRepository {
        return chat ?: synchronized(this) {
            chat ?: EngineChatRepository(context).also { chat = it }
        }
    }

    fun media(context: Context): com.polli.domain.repository.MediaRepository {
        return media ?: synchronized(this) {
            media ?: EngineMediaRepository(context).also { media = it }
        }
    }
}
