package com.polli.android.newchat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import org.thoughtcrime.securesms.connect.DcHelper

class ContactPickerActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                ContactPickerScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, ContactPickerActivity::class.java)
    }
}

@Composable
private fun ContactPickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dc = remember { DcHelper.getContext(context) }
    var query by remember { mutableStateOf("") }
    val contacts = remember(query) { loadContacts(dc, query) }

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
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(contacts, key = { it.id }) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val chatId = dc.createChatByContactId(contact.id)
                            if (chatId > 0) AppNav.openChat(context, chatId)
                            (context as? ContactPickerActivity)?.finish()
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
                    Column {
                        Text(contact.displayName, color = PolliColors.White85, style = MaterialTheme.typography.bodyMedium)
                        Text(contact.addr, color = PolliColors.White33, style = MaterialTheme.typography.bodySmall)
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
