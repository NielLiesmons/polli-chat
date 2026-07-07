package com.polli.android.newchat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.b44t.messenger.DcContact
import com.b44t.messenger.DcContext
import com.polli.android.BaseComposeActivity
import com.polli.android.navigation.AppNav
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.accent
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.PolliAvatar
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton

import com.polli.android.platform.EngineBridge

/**
 * Polli Compose contact picker. Replaces the legacy Signal ContactSelectionActivity family.
 *
 * Modes:
 *  - [MODE_OPEN_CHAT] (default): tapping a contact opens/creates a 1:1 chat.
 *  - [MODE_PICK_SINGLE]: returns [RESULT_CONTACT_ID] for the tapped contact.
 *  - [MODE_PICK_MULTI]: multi-select seeded with [EXTRA_PRESELECTED]; returns [RESULT_SELECTED]
 *    (final checked set) and [RESULT_DESELECTED] (preselected contacts that were unchecked).
 */
class ContactPickerActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = AppPrefs(this)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_OPEN_CHAT
        val preselected =
            intent.getIntegerArrayListExtra(EXTRA_PRESELECTED)?.toSet() ?: emptySet()
        setContent {
            PolliTheme(prefs = prefs) {
                ContactPickerScreen(
                    mode = mode,
                    preselected = preselected,
                    onBack = { finish() },
                    onPickSingle = { id ->
                        setResult(Activity.RESULT_OK, Intent().putExtra(RESULT_CONTACT_ID, id))
                        finish()
                    },
                    onPickMulti = { selected ->
                        val deselected = preselected - selected
                        setResult(
                            Activity.RESULT_OK,
                            Intent().apply {
                                putIntegerArrayListExtra(RESULT_SELECTED, ArrayList(selected))
                                putIntegerArrayListExtra(RESULT_DESELECTED, ArrayList(deselected))
                            },
                        )
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val MODE_OPEN_CHAT = "open_chat"
        const val MODE_PICK_SINGLE = "pick_single"
        const val MODE_PICK_MULTI = "pick_multi"

        const val EXTRA_MODE = "mode"
        const val EXTRA_PRESELECTED = "preselected"

        const val RESULT_CONTACT_ID = "result_contact_id"
        const val RESULT_SELECTED = "result_selected"
        const val RESULT_DESELECTED = "result_deselected"

        fun intent(context: Context): Intent = Intent(context, ContactPickerActivity::class.java)

        fun pickSingle(context: Context): Intent =
            intent(context).putExtra(EXTRA_MODE, MODE_PICK_SINGLE)

        fun pickMulti(context: Context, preselected: List<Int>): Intent =
            intent(context)
                .putExtra(EXTRA_MODE, MODE_PICK_MULTI)
                .putIntegerArrayListExtra(EXTRA_PRESELECTED, ArrayList(preselected))
    }
}

@Composable
private fun ContactPickerScreen(
    mode: String,
    preselected: Set<Int>,
    onBack: () -> Unit,
    onPickSingle: (Int) -> Unit,
    onPickMulti: (Set<Int>) -> Unit,
) {
    val context = LocalContext.current
    val dc = remember { EngineBridge.getContext(context) }
    var query by remember { mutableStateOf("") }
    val contacts = remember(query) { loadContacts(dc, query) }
    var checked by remember { mutableStateOf(preselected) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .padding(top = AppInsets.statusBarTop())
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundBackButton(onClick = onBack)
            Spacer(modifier = Modifier.padding(8.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PolliColors.Gray33)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = PolliColors.White85),
                cursorBrush = SolidColor(PolliColors.White),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Search contacts", color = PolliColors.White33)
                    }
                    inner()
                },
            )
            if (mode == ContactPickerActivity.MODE_PICK_MULTI) {
                Spacer(modifier = Modifier.padding(6.dp))
                Text(
                    "Done",
                    color = accent().solid,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onPickMulti(checked) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(contacts, key = { it.id }) { contact ->
                val isChecked = contact.id in checked
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when (mode) {
                                ContactPickerActivity.MODE_PICK_SINGLE -> onPickSingle(contact.id)
                                ContactPickerActivity.MODE_PICK_MULTI ->
                                    checked =
                                        if (isChecked) checked - contact.id else checked + contact.id
                                else -> {
                                    val chatId = dc.createChatByContactId(contact.id)
                                    if (chatId > 0) AppNav.openChat(context, chatId)
                                    (context as? Activity)?.finish()
                                }
                            }
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PolliAvatar(
                        name = contact.displayName,
                        seed = contact.addr,
                        size = 44.dp,
                        contactId = contact.id,
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(contact.displayName, color = PolliColors.White85, style = MaterialTheme.typography.bodyMedium)
                        Text(contact.addr, color = PolliColors.White33, style = MaterialTheme.typography.bodySmall)
                    }
                    if (mode == ContactPickerActivity.MODE_PICK_MULTI) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isChecked) accent().solid else PolliColors.Gray33),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isChecked) {
                                Text(
                                    "\u2713",
                                    color = PolliColors.White,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PickerContact(val id: Int, val displayName: String, val addr: String)

private fun loadContacts(dc: DcContext, query: String): List<PickerContact> {
    val flags = DcContext.DC_GCL_ADD_SELF
    val ids = dc.getContacts(flags, query.ifBlank { null }) ?: return emptyList()
    val out = ArrayList<PickerContact>()
    for (id in ids) {
        if (id <= DcContact.DC_CONTACT_ID_LAST_SPECIAL) continue
        val c = dc.getContact(id)
        if (c.id <= 0) continue
        out.add(
            PickerContact(
                id = id,
                displayName = c.displayName?.ifBlank { c.addr } ?: c.addr,
                addr = c.addr,
            ),
        )
    }
    return out
}
