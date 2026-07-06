package com.polli.android.newchat

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import chat.delta.rpc.RpcException
import com.polli.android.BaseComposeActivity
import com.polli.android.media.ImageEditLauncher
import com.polli.android.navigation.AppNav
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.theme.accent
import com.polli.android.ui.AppInsets
import com.polli.android.ui.PolliAvatar
import com.polli.android.ui.RoundBackButton
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.profiles.AvatarHelper

private sealed class GroupCreateMode {
    data class Create(val broadcast: Boolean) : GroupCreateMode()
    data class Edit(
        val chatId: Int,
        val broadcast: Boolean,
        val initialName: String,
        val initialDescription: String,
    ) : GroupCreateMode()
    data class Clone(
        val sourceChatId: Int,
        val broadcast: Boolean,
        val initialName: String,
        val contactIds: List<Int>,
    ) : GroupCreateMode()
}

/** Compose group/channel create, edit, and clone — replaces Java GroupCreateActivity. */
class GroupCreateActivity : BaseComposeActivity() {

    private var pendingAvatar by mutableStateOf<Bitmap?>(null)
    private var avatarChanged = false

    private val imageEditor = ImageEditLauncher(
        activity = this,
        onEdited = { uri -> applyAvatarUri(uri) },
    )

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageEditor.launch(uri, cropAvatar = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editChatId = intent.getIntExtra(EXTRA_EDIT_CHAT_ID, 0)
        val cloneChatId = intent.getIntExtra(EXTRA_CLONE_CHAT_ID, 0)
        val broadcastExtra = intent.getBooleanExtra(EXTRA_BROADCAST, false)
        val dc = DcHelper.getContext(this)
        val mode: GroupCreateMode =
            when {
                editChatId != 0 -> {
                    val chat = dc.getChat(editChatId)
                    GroupCreateMode.Edit(
                        chatId = editChatId,
                        broadcast = chat.isOutBroadcast,
                        initialName = chat.name.orEmpty(),
                        initialDescription = loadDescription(editChatId),
                    )
                }
                cloneChatId != 0 -> {
                    val chat = dc.getChat(cloneChatId)
                    GroupCreateMode.Clone(
                        sourceChatId = cloneChatId,
                        broadcast = chat.isOutBroadcast,
                        initialName = chat.name.orEmpty(),
                        contactIds = dc.getChatContacts(cloneChatId).toList(),
                    )
                }
                else -> GroupCreateMode.Create(broadcast = broadcastExtra)
            }
        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                GroupCreateScreen(
                    mode = mode,
                    onBack = { finish() },
                    onPickAvatar = { pickAvatar.launch("image/*") },
                    onSave = { name, description -> save(mode, name, description) },
                )
            }
        }
    }

    private fun loadDescription(chatId: Int): String {
        return try {
            val rpc = DcHelper.getRpc(this)
            rpc.getChatDescription(rpc.selectedAccountId, chatId).orEmpty()
        } catch (_: RpcException) {
            ""
        }
    }

    private fun applyAvatarUri(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { stream ->
            pendingAvatar = BitmapFactory.decodeStream(stream)
            avatarChanged = true
        }
    }

    private fun save(mode: GroupCreateMode, name: String, description: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_chat_name, Toast.LENGTH_LONG).show()
            return
        }
        val dc = DcHelper.getContext(this)
        val rpc = DcHelper.getRpc(this)
        try {
            when (mode) {
                is GroupCreateMode.Edit -> {
                    dc.setChatName(mode.chatId, trimmed)
                    rpc.setChatDescription(rpc.selectedAccountId, mode.chatId, description.trim())
                    if (avatarChanged && pendingAvatar != null) {
                        AvatarHelper.setGroupAvatar(this, mode.chatId, pendingAvatar)
                    }
                    setResult(RESULT_OK)
                    finish()
                }
                is GroupCreateMode.Create -> {
                    createChat(dc, rpc, trimmed, description, mode.broadcast, emptyList())
                }
                is GroupCreateMode.Clone -> {
                    createChat(dc, rpc, trimmed, description, mode.broadcast, mode.contactIds)
                }
            }
        } catch (e: RpcException) {
            Toast.makeText(this, e.message ?: getString(R.string.error), Toast.LENGTH_LONG).show()
        }
    }

    private fun createChat(
        dc: com.b44t.messenger.DcContext,
        rpc: chat.delta.rpc.Rpc,
        name: String,
        description: String,
        broadcast: Boolean,
        memberIds: List<Int>,
    ) {
        val accId = rpc.selectedAccountId
        val chatId = if (broadcast) {
            rpc.createBroadcast(accId, name)
        } else {
            rpc.createGroupChat(accId, name, false)
        }
        if (chatId == null || chatId <= 0) return
        if (description.isNotBlank()) {
            rpc.setChatDescription(accId, chatId, description.trim())
        }
        for (contactId in memberIds) {
            dc.addContactToChat(chatId, contactId)
        }
        if (avatarChanged && pendingAvatar != null) {
            AvatarHelper.setGroupAvatar(this, chatId, pendingAvatar)
        }
        AppNav.openChat(this, chatId)
        finish()
    }

    companion object {
        const val EXTRA_EDIT_CHAT_ID = "edit_group_chat_id"
        const val EXTRA_CLONE_CHAT_ID = "clone_chat"
        private const val EXTRA_BROADCAST = "broadcast"

        @JvmStatic
        fun intent(context: Context, broadcast: Boolean): Intent =
            Intent(context, GroupCreateActivity::class.java).apply {
                putExtra(EXTRA_BROADCAST, broadcast)
            }

        @JvmStatic
        fun intentEdit(context: Context, chatId: Int): Intent =
            Intent(context, GroupCreateActivity::class.java).apply {
                putExtra(EXTRA_EDIT_CHAT_ID, chatId)
            }

        @JvmStatic
        fun intentClone(context: Context, chatId: Int): Intent =
            Intent(context, GroupCreateActivity::class.java).apply {
                putExtra(EXTRA_CLONE_CHAT_ID, chatId)
            }
    }
}

@Composable
private fun GroupCreateScreen(
    mode: GroupCreateMode,
    onBack: () -> Unit,
    onPickAvatar: () -> Unit,
    onSave: (name: String, description: String) -> Unit,
) {
    val context = LocalContext.current
    val dc = remember { DcHelper.getContext(context) }
    val initialName = when (mode) {
        is GroupCreateMode.Create -> ""
        is GroupCreateMode.Edit -> mode.initialName
        is GroupCreateMode.Clone -> mode.initialName
    }
    val initialDescription = when (mode) {
        is GroupCreateMode.Edit -> mode.initialDescription
        else -> ""
    }
    val isEdit = mode is GroupCreateMode.Edit
    val broadcast = when (mode) {
        is GroupCreateMode.Create -> mode.broadcast
        is GroupCreateMode.Edit -> mode.broadcast
        is GroupCreateMode.Clone -> mode.broadcast
    }
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    val title = when {
        isEdit -> "Edit group"
        broadcast -> "New channel"
        else -> "New group"
    }
    val avatarChatId = when (mode) {
        is GroupCreateMode.Edit -> mode.chatId
        is GroupCreateMode.Clone -> mode.sourceChatId
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .padding(top = AppInsets.statusBarTop())
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBackButton(onClick = onBack)
            Spacer(modifier = Modifier.padding(12.dp))
            Text(title, color = PolliColors.White85, style = MaterialTheme.typography.titleLarge)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clickable(onClick = onPickAvatar),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PolliAvatar(
                name = name.ifBlank { "Group" },
                seed = name.ifBlank { "group" },
                size = 72.dp,
                chatId = avatarChatId,
                dcContext = dc,
            )
            Text("Tap to change photo", color = PolliColors.White33, modifier = Modifier.padding(top = 8.dp))
        }
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PolliColors.Gray33)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = PolliColors.White85),
            cursorBrush = SolidColor(PolliColors.White),
            decorationBox = { inner ->
                if (name.isEmpty()) {
                    Text(
                        if (broadcast) "Channel name" else "Group name",
                        color = PolliColors.White33,
                    )
                }
                inner()
            },
        )
        if (!broadcast) {
            Spacer(modifier = Modifier.padding(12.dp))
            BasicTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PolliColors.Gray33)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = PolliColors.White85),
                cursorBrush = SolidColor(PolliColors.White),
                decorationBox = { inner ->
                    if (description.isEmpty()) {
                        Text("Description (optional)", color = PolliColors.White33)
                    }
                    inner()
                },
            )
        }
        Spacer(modifier = Modifier.padding(24.dp))
        TextButton(
            onClick = { onSave(name, description) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                if (isEdit) "Save" else "Create",
                color = accent().light,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.padding(AppInsets.navigationBarBottom() + 24.dp))
    }
}
