package com.polli.core.chat

/** Tabs in opened group chat — carousel under the title bar. */
enum class ChatDetailTab(val label: String) {
    Search("Search"),
    Info("Info"),
    Chat("Chat"),
    Apps("Apps"),
    Media("Media"),
    Audio("Audio"),
    Files("Files"),
    Tasks("Tasks"),
    Docs("Docs"),
}

fun tabsForChat(isGroup: Boolean, isBroadcast: Boolean): List<ChatDetailTab> {
    if (isBroadcast || !isGroup) {
        return listOf(ChatDetailTab.Chat)
    }
    return listOf(
        ChatDetailTab.Search,
        ChatDetailTab.Info,
        ChatDetailTab.Chat,
        ChatDetailTab.Apps,
        ChatDetailTab.Media,
        ChatDetailTab.Audio,
        ChatDetailTab.Files,
        ChatDetailTab.Tasks,
        ChatDetailTab.Docs,
    )
}
