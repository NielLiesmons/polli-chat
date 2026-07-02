package com.polli.android.notes

import com.b44t.messenger.DcContact
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcMsg

/** Delta Chat Saved Messages (self-talk) as Polli Notes. */
class NotesStore(private val dc: DcContext) {

    fun selfTalkChatId(): Int = dc.createChatByContactId(DcContact.DC_CONTACT_ID_SELF)

    fun loadNotes(): List<Note> {
        val ids = dc.getChatMsgs(selfTalkChatId(), 0, 0) ?: intArrayOf()
        return ids
            .filter { it > DcMsg.DC_MSG_ID_DAYMARKER }
            .mapNotNull { id -> noteFromMsgId(id) }
            .asReversed()
    }

    fun noteFromMsgId(msgId: Int): Note? {
        val msg = dc.getMsg(msgId)
        if (!msg.isOk || msg.isInfo) return null
        val text = msg.text?.trim().orEmpty()
        if (text.isEmpty() && !msg.hasFile()) return null
        val body = if (text.isNotEmpty()) {
            text
        } else {
            msg.filename?.takeIf { it.isNotBlank() } ?: "(attachment)"
        }
        val (title, preview) = NoteText.parse(body)
        return Note(
            msgId = msg.id,
            title = title,
            preview = preview,
            body = body,
            timestamp = msg.timestamp,
            hasAttachment = msg.hasFile(),
        )
    }

    fun createNote(text: String): Int = dc.sendTextMsg(selfTalkChatId(), text)

    fun updateNote(msgId: Int, text: String) {
        dc.sendEditRequest(msgId, text)
    }

    fun deleteNote(msgId: Int) {
        dc.deleteMsgs(intArrayOf(msgId))
    }
}
