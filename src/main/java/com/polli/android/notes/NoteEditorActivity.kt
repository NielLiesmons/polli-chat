package com.polli.android.notes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import com.polli.android.BaseComposeActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabTheme

class NoteEditorActivity : BaseComposeActivity() {

    private val viewModel: NoteEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val msgId = intent.getIntExtra(EXTRA_MSG_ID, -1)
        viewModel.bind(msgId)

        setContent {
            val prefs = remember { AppPrefs(this@NoteEditorActivity) }
            LabTheme(prefs = prefs) {
                NoteEditorScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onDeleted = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_MSG_ID = "note_msg_id"

        fun intent(context: Context, msgId: Int = -1): Intent =
            Intent(context, NoteEditorActivity::class.java).apply {
                if (msgId > 0) putExtra(EXTRA_MSG_ID, msgId)
            }
    }
}
