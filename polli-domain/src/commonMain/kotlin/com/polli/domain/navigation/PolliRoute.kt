package com.polli.domain.navigation

sealed interface PolliRoute {
    data object Home : PolliRoute
    data class Chat(val chatId: Int, val accountId: Int = -1) : PolliRoute
    data object Archive : PolliRoute
    data object Welcome : PolliRoute
    data object Profiles : PolliRoute
    data object ProfileEdit : PolliRoute
    data object NewConversation : PolliRoute
    data object AppSettings : PolliRoute
    data class ChannelStories(val chatId: Int) : PolliRoute
    data class MediaPreview(val messageId: Int, val chatId: Int) : PolliRoute
    data class ContactProfile(val contactId: Int) : PolliRoute
}
