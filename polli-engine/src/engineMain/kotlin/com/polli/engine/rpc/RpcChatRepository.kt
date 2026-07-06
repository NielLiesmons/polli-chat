package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException
import chat.delta.rpc.types.ChatType
import com.polli.core.chat.ChatListFlags
import com.polli.domain.model.chat.ChatSessionInfo
import com.polli.domain.model.ArchiveLinkRules
import com.polli.domain.model.ArchiveLinkState
import com.polli.domain.model.InboxItem
import com.polli.domain.repository.ChatRepository

class RpcChatRepository(
    private val rpc: Rpc,
    private val accountId: Int,
    private val eventLoop: RpcEventLoop,
) : ChatRepository {
    override fun loadInbox(query: String?): List<InboxItem> =
        RpcChatListMapper.load(rpc, accountId, ChatListFlags.INBOX, query)

    override fun loadArchived(query: String?): List<InboxItem> =
        RpcChatListMapper.load(rpc, accountId, ChatListFlags.ARCHIVED_ONLY, query)

    override fun archiveLinkState(): ArchiveLinkState {
        return try {
            val archived =
                rpc.getChatlistEntries(accountId, ChatListFlags.ARCHIVED_ONLY, null, null)
                    ?: emptyList()
            val visible = archived.isNotEmpty()
            val unread =
                if (visible) {
                    rpc.getFreshMsgCnt(accountId, ArchiveLinkRules.ARCHIVED_LINK_CHAT_ID).coerceAtLeast(0)
                } else {
                    0
                }
            ArchiveLinkRules.linkState(archived.size, unread)
        } catch (_: RpcException) {
            ArchiveLinkState(visible = false, unreadCount = 0)
        }
    }

    override fun getFreshMessageCount(chatId: Int): Int =
        try {
            rpc.getFreshMsgCnt(accountId, chatId).coerceAtLeast(0)
        } catch (_: RpcException) {
            0
        }

    override fun observeInbox(onUpdate: () -> Unit): AutoCloseable =
        eventLoop.addListener(onUpdate)

    override fun getSession(chatId: Int): ChatSessionInfo? {
        return try {
            val chat = rpc.getFullChatById(accountId, chatId) ?: return null
            val type = chat.chatType
            ChatSessionInfo(
                chatId = chatId,
                name = chat.name.orEmpty().ifBlank { "Chat" },
                canSend = chat.canSend == true,
                isEncrypted = chat.isEncrypted == true,
                isMultiUser = type == ChatType.Group || type == ChatType.Mailinglist,
                isSelfTalk = chat.isSelfTalk == true,
                isOutBroadcast = type == ChatType.OutBroadcast,
                isInBroadcast = type == ChatType.InBroadcast,
            )
        } catch (_: RpcException) {
            null
        }
    }

    override fun createChatByContactId(contactId: Int): Int? {
        if (contactId <= 0) return null
        return try {
            rpc.createChatByContactId(accountId, contactId)
        } catch (_: RpcException) {
            null
        }
    }
}
