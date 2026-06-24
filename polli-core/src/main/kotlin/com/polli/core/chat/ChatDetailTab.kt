package com.polli.core.chat

/** Tabs in opened group chat — left-aligned row under the title bar. */
enum class ChatDetailTab(val label: String) {
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
    return ChatDetailTab.entries.toList()
}
