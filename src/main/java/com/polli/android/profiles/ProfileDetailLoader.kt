package com.polli.android.profiles

import android.content.Context
import com.b44t.messenger.DcChat
import com.b44t.messenger.DcChatlist
import com.b44t.messenger.DcContact
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcLot
import chat.delta.rpc.RpcException
import com.polli.android.R
import com.polli.android.platform.EngineBridge
import com.polli.android.platform.PlatformDates

data class ProfileMemberRow(
    val contactId: Int,
    val name: String,
    val addr: String? = null,
)

data class SharedChatRow(
    val chatId: Int,
    val title: String,
    val subtitle: String,
)

data class ProfileDetailUiState(
    val screenTitle: String,
    val displayName: String,
    val seed: String,
    val chatId: Int,
    val contactId: Int,
    val statusText: String?,
    val showAllMedia: Boolean,
    val showSendMessage: Boolean,
    val lastSeenText: String?,
    val blocked: Boolean,
    val introducedByLabel: String?,
    val introducedByContactId: Int,
    val members: List<ProfileMemberRow>,
    val showAddMember: Boolean,
    val showQrInvite: Boolean,
    val sharedChats: List<SharedChatRow>,
    val isSelfProfile: Boolean,
    val isMultiUser: Boolean,
    val isDeviceTalk: Boolean,
    val isMailingList: Boolean,
    val isOutBroadcast: Boolean,
    val isInBroadcast: Boolean,
    val canEditGroup: Boolean,
    val canClone: Boolean,
    val canReceiveNotifications: Boolean,
    val isContactProfile: Boolean,
    val isMuted: Boolean,
    val isContactBlocked: Boolean,
)

object ProfileDetailLoader {
    fun load(context: Context, chatIdIn: Int, contactIdIn: Int): ProfileDetailUiState {
        val dc = EngineBridge.getContext(context)
        var chatId = chatIdIn
        var contactId = contactIdIn
        var contactIsBot = false

        if (contactId != 0) {
            val contact = dc.getContact(contactId)
            chatId = dc.getChatIdByContactId(contactId)
            contactIsBot = contact.isBot
        }

        var chat: DcChat? = null
        if (chatId != 0) {
            chat = dc.getChat(chatId)
            if (!chat.isMultiUser) {
                val members = dc.getChatContacts(chatId)
                contactId = if (members.isNotEmpty()) members[0] else 0
            }
        }

        val contact = if (contactId != 0) dc.getContact(contactId) else null
        val chatIsMultiUser = chat?.isMultiUser == true
        val chatIsDeviceTalk = chat?.isDeviceTalk == true
        val chatIsMailingList = chat?.isMailingList == true
        val chatIsInBroadcast = chat?.isInBroadcast == true
        val chatIsOutBroadcast = chat?.isOutBroadcast == true
        val isSelfTalk = chat?.isSelfTalk == true
        val isContactProfile = contactId != 0 && (chatId == 0 || !chatIsMultiUser)
        val isSelfProfile = isContactProfile && contactId == DcContact.DC_CONTACT_ID_SELF

        val screenTitle =
            when {
                chatIsMailingList -> context.getString(R.string.mailing_list)
                chatIsOutBroadcast || chatIsInBroadcast -> context.getString(R.string.channel)
                chatIsMultiUser -> context.getString(R.string.tab_group)
                contactIsBot -> context.getString(R.string.bot)
                !chatIsDeviceTalk && !isSelfProfile -> context.getString(R.string.tab_contact)
                else -> context.getString(R.string.profile)
            }

        val displayName =
            when {
                chat != null && chatIsMultiUser -> chat.name.orEmpty()
                contact != null -> contact.displayName
                else -> ""
            }
        val seed = contact?.addr?.takeIf { it.isNotBlank() } ?: displayName

        val statusText = buildStatusText(context, dc, chat, contact, isSelfTalk, chatIsDeviceTalk)
        val lastSeenText = buildLastSeenText(context, contact, chatIsDeviceTalk, isSelfTalk)
        val blocked = contact?.isBlocked == true

        var introducedByLabel: String? = null
        var introducedByContactId = 0
        if (contact != null && !chatIsDeviceTalk && !isSelfTalk) {
            val verifierId = contact.verifierId
            if (verifierId != 0 && verifierId != DcContact.DC_CONTACT_ID_SELF) {
                introducedByContactId = verifierId
                introducedByLabel =
                    context.getString(
                        R.string.verified_by,
                        dc.getContact(verifierId).displayName,
                    )
            }
        }

        val members = buildMembers(context, dc, chat, chatId, chatIsMultiUser, chatIsInBroadcast, chatIsMailingList)
        val sharedChats = buildSharedChats(context, dc, contactId, chatIsDeviceTalk, isSelfTalk, chatIsMultiUser)

        val canReceive =
            when {
                chatId == 0 -> false
                chatIsOutBroadcast -> false
                chatIsMultiUser -> chat?.isEncrypted == true && chat.canSend() && !chatIsMailingList
                else -> true
            }

        val canEditGroup =
            chatIsMultiUser &&
                chat != null &&
                !chatIsMailingList &&
                !chatIsInBroadcast &&
                (chat.canSend() || chatIsOutBroadcast)

        return ProfileDetailUiState(
            screenTitle = screenTitle,
            displayName = displayName,
            seed = seed,
            chatId = chatId,
            contactId = contactId,
            statusText = statusText,
            showAllMedia = chatId > 0,
            showSendMessage = contact != null && !chatIsDeviceTalk && !isSelfTalk && chatId == 0,
            lastSeenText = lastSeenText,
            blocked = blocked,
            introducedByLabel = introducedByLabel,
            introducedByContactId = introducedByContactId,
            members = members,
            showAddMember =
                members.isNotEmpty() &&
                    chat?.canSend() == true &&
                    chat.isEncrypted &&
                    !chatIsOutBroadcast,
            showQrInvite =
                members.isNotEmpty() &&
                    chat?.canSend() == true &&
                    chat.isEncrypted,
            sharedChats = sharedChats,
            isSelfProfile = isSelfProfile,
            isMultiUser = chatIsMultiUser,
            isDeviceTalk = chatIsDeviceTalk,
            isMailingList = chatIsMailingList,
            isOutBroadcast = chatIsOutBroadcast,
            isInBroadcast = chatIsInBroadcast,
            canEditGroup = canEditGroup,
            canClone =
                chatIsMultiUser &&
                    !chatIsInBroadcast &&
                    !chatIsOutBroadcast &&
                    !chatIsMailingList,
            canReceiveNotifications = canReceive,
            isContactProfile = isContactProfile,
            isMuted = chat?.isMuted == true,
            isContactBlocked = contact?.isBlocked == true,
        )
    }

    private fun buildStatusText(
        context: Context,
        dc: DcContext,
        chat: DcChat?,
        contact: DcContact?,
        isSelfTalk: Boolean,
        isDeviceTalk: Boolean,
    ): String? {
        if (isSelfTalk) return context.getString(R.string.saved_messages_explain)
        if (contact != null && !isDeviceTalk) {
            return contact.status?.takeIf { it.isNotBlank() }
        }
        if (chat != null && chat.isEncrypted) {
            return try {
                val rpc = EngineBridge.getRpc(context)
                rpc.getChatDescription(rpc.selectedAccountId, chat.id)?.takeIf { it.isNotBlank() }
            } catch (_: RpcException) {
                null
            }
        }
        return null
    }

    private fun buildLastSeenText(
        context: Context,
        contact: DcContact?,
        isDeviceTalk: Boolean,
        isSelfTalk: Boolean,
    ): String? {
        if (contact == null || isDeviceTalk || isSelfTalk) return null
        val lastSeen = contact.lastSeen
        return if (lastSeen == 0L) {
            context.getString(R.string.last_seen_unknown)
        } else {
            context.getString(
                R.string.last_seen_at,
                PlatformDates.extendedTimeSpan(context, lastSeen),
            )
        }
    }

    private fun buildMembers(
        context: Context,
        dc: DcContext,
        chat: DcChat?,
        chatId: Int,
        isMultiUser: Boolean,
        isInBroadcast: Boolean,
        isMailingList: Boolean,
    ): List<ProfileMemberRow> {
        if (!isMultiUser || isInBroadcast || isMailingList || chatId <= 0) return emptyList()
        val rows = mutableListOf<ProfileMemberRow>()
        if (chat?.canSend() == true && chat.isEncrypted) {
            if (!chat.isOutBroadcast) {
                rows +=
                    ProfileMemberRow(
                        DcContact.DC_CONTACT_ID_ADD_MEMBER,
                        context.getString(R.string.group_add_members),
                    )
            }
            rows +=
                ProfileMemberRow(
                    DcContact.DC_CONTACT_ID_QR_INVITE,
                    context.getString(R.string.qrshow_title),
                )
        }
        for (memberId in dc.getChatContacts(chatId)) {
            val member = dc.getContact(memberId)
            rows += ProfileMemberRow(memberId, member.displayName, member.addr)
        }
        return rows
    }

    private fun buildSharedChats(
        context: Context,
        dc: DcContext,
        contactId: Int,
        isDeviceTalk: Boolean,
        isSelfTalk: Boolean,
        isMultiUser: Boolean,
    ): List<SharedChatRow> {
        if (isDeviceTalk || isSelfTalk || isMultiUser || contactId <= 0 || contactId == DcContact.DC_CONTACT_ID_SELF) {
            return emptyList()
        }
        val chatlist: DcChatlist = dc.getChatlist(0, null, contactId) ?: return emptyList()
        val rows = mutableListOf<SharedChatRow>()
        for (i in 0 until chatlist.getCnt()) {
            val sharedChatId = chatlist.getChatId(i)
            val chat = dc.getChat(sharedChatId)
            val summary: DcLot = chatlist.getSummary(i, chat)
            rows +=
                SharedChatRow(
                    chatId = sharedChatId,
                    title = chat.name.orEmpty(),
                    subtitle = summary.text1.orEmpty(),
                )
        }
        return rows
    }
}
