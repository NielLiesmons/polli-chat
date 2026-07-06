package com.polli.android.notes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.polli.android.platform.EngineBridge

class NoteEditorViewModel(application: Application) : AndroidViewModel(application) {

    var msgId by mutableIntStateOf(-1)
        private set
    var body by mutableStateOf("")
        private set
    var showPreview by mutableStateOf(true)
        private set

    private var initialBody = ""
    private lateinit var store: NotesStore

    val isNew: Boolean get() = msgId <= 0
    val hasChanges: Boolean get() = body.trim() != initialBody.trim()

    fun bind(msgId: Int) {
        this.msgId = msgId
        val dc = EngineBridge.getContext(getApplication())
        store = NotesStore(dc)
        body = if (msgId > 0) {
            dc.getMsg(msgId).text?.orEmpty() ?: ""
        } else {
            ""
        }
        initialBody = body
    }

    fun updateBody(text: String) {
        body = text
    }

    fun togglePreview() {
        showPreview = !showPreview
    }

    /** @return saved message id, or -1 if nothing was persisted */
    fun save(): Int {
        val text = body.trim()
        if (text.isEmpty()) return -1
        return if (msgId > 0) {
            if (hasChanges) {
                store.updateNote(msgId, text)
            }
            msgId
        } else {
            val id = store.createNote(text)
            msgId = id
            initialBody = text
            id
        }
    }

    fun delete() {
        if (msgId > 0) {
            store.deleteNote(msgId)
        }
    }
}
