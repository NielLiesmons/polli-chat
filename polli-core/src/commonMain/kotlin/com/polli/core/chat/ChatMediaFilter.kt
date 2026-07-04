package com.polli.core.chat

/** In-chat media browser tabs — maps to engine getChatMedia type filters. */
enum class ChatMediaFilter(val label: String, val type1: Int, val type2: Int, val type3: Int) {
    Media("Media", MsgTypes.IMAGE, MsgTypes.GIF, MsgTypes.VIDEO),
    Audio("Audio", MsgTypes.AUDIO, MsgTypes.VOICE, 0),
    Files("Files", MsgTypes.FILE, MsgTypes.WEBXDC, 0),
    ;

    val isGrid: Boolean get() = this == Media
}
