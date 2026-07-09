package com.polli.core.chat

/** Tabs in opened group chat — carousel under the title bar. */
enum class ChatDetailTab(val label: String) {
    Search("Search"),
    Info("Info"),
    Chat("Chat"),
    Apps("Apps"),
    Tasks("Tasks"),
    Files("Files"),
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
        ChatDetailTab.Tasks,
        ChatDetailTab.Files,
        ChatDetailTab.Docs,
    )
}
