package com.polli.android.profiles

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.delta.rpc.RpcException
import com.b44t.messenger.DcContact
import com.b44t.messenger.DcContext
import com.b44t.messenger.DcEvent
import com.polli.android.BaseAppCompatComposeActivity
import com.polli.android.HomeRelayingActivity
import com.polli.android.media.ChatAllMediaActivity
import com.polli.android.media.MediaPreviewActivity
import com.polli.android.navigation.AppNav
import com.polli.android.newchat.GroupCreateActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliTheme
import com.polli.android.platform.EngineBridge
import com.polli.android.newchat.ContactPickerActivity
import com.polli.android.ui.MuteDurationDialog
import com.polli.android.qr.QrShowActivity
import com.polli.android.platform.PlatformClipboard
import com.polli.android.platform.PlatformLegacyUtil
import com.polli.android.platform.PlatformMedia
import com.polli.android.platform.PlatformPrefs
import com.polli.android.platform.PlatformShare
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.connect.DcEventCenter
import java.io.File
import java.util.Collections

/** Compose contact/group profile — replaces Java ProfileActivity + ProfileFragment. */
class ProfileDetailActivity : BaseAppCompatComposeActivity(), DcEventCenter.DcEventDelegate {
    private lateinit var dcContext: com.b44t.messenger.DcContext
    private lateinit var rpc: chat.delta.rpc.Rpc

    private var inputChatId = 0
    private var inputContactId = 0

    private var uiState by mutableStateOf<ProfileDetailUiState?>(null)
    private var selectedMembers by mutableStateOf(setOf<Int>())

    private val pickMembers =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data ?: return@registerForActivityResult
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val selected = data.getIntegerArrayListExtra(ContactPickerActivity.RESULT_SELECTED)
            val deselected =
                data.getIntegerArrayListExtra(ContactPickerActivity.RESULT_DESELECTED)
            val chatId = uiState?.chatId ?: return@registerForActivityResult
            PlatformLegacyUtil.runOnAnyBackgroundThread {
                deselected?.forEach { contactId ->
                    for (memberId in dcContext.getChatContacts(chatId)) {
                        if (memberId == contactId) {
                            dcContext.removeContactFromChat(chatId, memberId)
                            break
                        }
                    }
                }
                selected?.forEach { contactId ->
                    dcContext.addContactToChat(chatId, contactId)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dcContext = EngineBridge.getContext(this)
        rpc = EngineBridge.getRpc(this)
        inputChatId = intent.getIntExtra(CHAT_ID_EXTRA, 0)
        inputContactId = intent.getIntExtra(CONTACT_ID_EXTRA, 0)
        reload()
        EngineBridge.getEventCenter(this).addObserver(DcContext.DC_EVENT_CHAT_MODIFIED, this)
        EngineBridge.getEventCenter(this).addObserver(DcContext.DC_EVENT_CONTACTS_CHANGED, this)
        EngineBridge.getEventCenter(this).addObserver(DcContext.DC_EVENT_MSGS_CHANGED, this)
        EngineBridge.getEventCenter(this).addObserver(DcContext.DC_EVENT_INCOMING_MSG, this)
        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                val state = uiState ?: return@PolliTheme
                ProfileDetailScreen(
                    state = state,
                    selectedMemberIds = selectedMembers,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onAvatarClick = { onEnlargeAvatar(state) },
                    onAllMediaClick = { openAllMedia(state.chatId) },
                    onSendMessageClick = { sendMessage(state.contactId) },
                    onIntroducedByClick = {
                        if (state.introducedByContactId != 0) {
                            startActivity(intentContact(this, state.introducedByContactId))
                        }
                    },
                    onMemberClick = { contactId -> onMemberClicked(contactId, state) },
                    onMemberLongClick = { contactId -> onMemberLongClicked(contactId, state) },
                    onSharedChatClick = { chatId ->
                        startActivity(AppNav.chatIntent(this, chatId))
                        finish()
                    },
                    onStatusLongClick = {
                        val text = state.statusText.orEmpty()
                        if (text.isNotBlank()) {
                            PlatformClipboard.copyText(this, text)
                            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRemoveSelectedMembers = { confirmRemoveSelectedMembers() },
                )
            }
        }
    }

    override fun onDestroy() {
        EngineBridge.getEventCenter(this).removeObservers(this)
        super.onDestroy()
    }

    override fun handleEvent(event: DcEvent) {
        reload()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val state = uiState ?: return super.onCreateOptionsMenu(menu)
        if (!state.isSelfProfile) {
            menuInflater.inflate(R.menu.profile_common, menu)
            menu.findItem(R.id.menu_clone)?.isVisible = state.canClone
            if (state.chatId == 0) {
                menu.findItem(R.id.menu_clone)?.isVisible = false
            }
            if (state.isDeviceTalk) {
                menu.findItem(R.id.edit_name)?.isVisible = false
                menu.findItem(R.id.show_encr_info)?.isVisible = false
                menu.findItem(R.id.share)?.isVisible = false
            } else if (state.isMultiUser) {
                menu.findItem(R.id.edit_name)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                if (!state.canEditGroup) {
                    menu.findItem(R.id.edit_name)?.isVisible = false
                }
                menu.findItem(R.id.share)?.isVisible = false
            }
            if (!state.canReceiveNotifications) {
                menu.findItem(R.id.menu_mute_notifications)?.isVisible = false
                menu.findItem(R.id.menu_sound)?.isVisible = false
                menu.findItem(R.id.menu_vibrate)?.isVisible = false
            }
            if (!state.isContactProfile || state.isDeviceTalk) {
                menu.findItem(R.id.block_contact)?.isVisible = false
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val state = uiState
        menu.findItem(R.id.block_contact)?.setTitle(
            if (state?.isContactBlocked == true) R.string.menu_unblock_contact else R.string.menu_block_contact,
        )
        if (state != null && state.chatId != 0) {
            menu.findItem(R.id.menu_mute_notifications)?.setTitle(
                if (state.isMuted) R.string.menu_unmute else R.string.menu_mute,
            )
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val state = uiState ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.menu_mute_notifications -> {
                onNotifyOnOff(state)
                true
            }
            R.id.menu_sound -> {
                onSoundSettings(state.chatId)
                true
            }
            R.id.menu_vibrate -> {
                onVibrateSettings(state.chatId)
                true
            }
            R.id.edit_name -> {
                onEditName(state)
                true
            }
            R.id.share -> {
                onShare(state.contactId)
                true
            }
            R.id.show_encr_info -> {
                onEncrInfo(state)
                true
            }
            R.id.block_contact -> {
                onBlockContact(state.contactId, state.isContactBlocked)
                true
            }
            R.id.menu_clone -> {
                startActivity(GroupCreateActivity.intentClone(this, state.chatId))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun reload() {
        uiState = ProfileDetailLoader.load(this, inputChatId, inputContactId)
        invalidateOptionsMenu()
    }

    private fun onMemberClicked(contactId: Int, state: ProfileDetailUiState) {
        when {
            selectedMembers.isNotEmpty() -> toggleMemberSelection(contactId)
            contactId == DcContact.DC_CONTACT_ID_ADD_MEMBER -> addMembers(state.chatId)
            contactId == DcContact.DC_CONTACT_ID_QR_INVITE -> qrInvite(state.chatId)
            contactId > DcContact.DC_CONTACT_ID_LAST_SPECIAL -> {
                startActivity(intentContact(this, contactId))
            }
        }
    }

    private fun confirmRemoveSelectedMembers() {
        val state = uiState ?: return
        val ids = selectedMembers.toList()
        if (ids.isEmpty()) return
        val names = ids.joinToString { dcContext.getContact(it).displayName }
        val chat = dcContext.getChat(state.chatId)
        val messageRes =
            if (chat.isOutBroadcast) R.string.ask_remove_from_channel else R.string.ask_remove_members
        val dialog =
            AlertDialog.Builder(this)
                .setMessage(getString(messageRes, names))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.remove_desktop) { _, _ ->
                    ids.forEach { dcContext.removeContactFromChat(state.chatId, it) }
                    selectedMembers = emptySet()
                    reload()
                }
                .show()
        PlatformLegacyUtil.redPositiveButton(dialog)
    }

    private fun onMemberLongClicked(contactId: Int, state: ProfileDetailUiState) {
        if (contactId <= DcContact.DC_CONTACT_ID_LAST_SPECIAL && contactId != DcContact.DC_CONTACT_ID_SELF) {
            return
        }
        val chat = dcContext.getChat(state.chatId)
        if (!chat.canSend() || !chat.isEncrypted) return
        if (selectedMembers.isEmpty()) {
            selectedMembers = setOf(contactId)
        } else {
            toggleMemberSelection(contactId)
        }
    }

    private fun toggleMemberSelection(contactId: Int) {
        selectedMembers =
            if (selectedMembers.contains(contactId)) {
                selectedMembers - contactId
            } else {
                selectedMembers + contactId
            }
        if (selectedMembers.isEmpty()) {
            // selection cleared
        }
    }

    private fun addMembers(chatId: Int) {
        val preselected = dcContext.getChatContacts(chatId).toList()
        pickMembers.launch(ContactPickerActivity.pickMulti(this, preselected))
    }

    private fun qrInvite(chatId: Int) {
        startActivity(
            Intent(this, QrShowActivity::class.java).apply {
                putExtra(QrShowActivity.CHAT_ID, chatId)
            },
        )
    }

    private fun sendMessage(contactId: Int) {
        val chatId = dcContext.createChatByContactId(contactId)
        if (chatId != 0) {
            startActivity(AppNav.chatIntent(this, chatId))
            finish()
        }
    }

    private fun openAllMedia(chatId: Int) {
        startActivity(ChatAllMediaActivity.intent(this, chatId))
    }

    private fun onEnlargeAvatar(state: ProfileDetailUiState) {
        val profileImagePath: String
        val title: String
        var enlarge = true
        if (state.chatId != 0) {
            val chat = dcContext.getChat(state.chatId)
            profileImagePath = chat.profileImage.orEmpty()
            title = chat.name.orEmpty()
            enlarge = chat.isEncrypted && !chat.isSelfTalk && !chat.isDeviceTalk
        } else {
            val contact = dcContext.getContact(state.contactId)
            profileImagePath = contact.profileImage.orEmpty()
            title = contact.displayName
        }
        val file = File(profileImagePath)
        if (enlarge && file.exists()) {
            val type = "image/" + profileImagePath.substringAfterLast('.', "jpeg")
            startActivity(
                MediaPreviewActivity.intentAvatar(
                    this,
                    file = file,
                    mimeType = type,
                    title = title,
                    editChatId =
                        if (state.isMultiUser && !state.isInBroadcast && !state.isMailingList) {
                            state.chatId
                        } else {
                            0
                        },
                ),
            )
        } else if (state.isMultiUser) {
            onEditName(state)
        }
    }

    private fun onNotifyOnOff(state: ProfileDetailUiState) {
        if (dcContext.getChat(state.chatId).isMuted) {
            dcContext.setChatMuteDuration(state.chatId, 0)
            reload()
        } else {
            MuteDurationDialog.show(this) { duration ->
                dcContext.setChatMuteDuration(state.chatId, duration)
                reload()
            }
        }
    }

    private fun onSoundSettings(chatId: Int) {
        var current = PlatformPrefs.getChatRingtone(this, dcContext.accountId, chatId)
        val defaultUri = PlatformPrefs.getNotificationRingtone(this)
        if (current == null) current = Settings.System.DEFAULT_NOTIFICATION_URI
        else if (current.toString().isEmpty()) current = null
        val intent =
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
            }
        ringtonePicker.launch(intent)
    }

    private val ringtonePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val state = uiState ?: return@registerForActivityResult
            if (result.resultCode != Activity.RESULT_OK || result.data == null) return@registerForActivityResult
            var value = result.data!!.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val defaultValue = PlatformPrefs.getNotificationRingtone(this)
            if (defaultValue == value) value = null else if (value == null) value = Uri.EMPTY
            PlatformPrefs.setChatRingtone(this, dcContext.accountId, state.chatId, value)
        }

    private fun onVibrateSettings(chatId: Int) {
        val checkedItem = PlatformPrefs.getChatVibrate(this, dcContext.accountId, chatId).id
        val selected = intArrayOf(checkedItem)
        AlertDialog.Builder(this)
            .setTitle(R.string.pref_vibrate)
            .setSingleChoiceItems(R.array.recipient_vibrate_entries, checkedItem) { _, which ->
                selected[0] = which
            }
            .setPositiveButton(R.string.ok) { _, _ ->
                PlatformPrefs.setChatVibrate(
                    this,
                    dcContext.accountId,
                    chatId,
                    PlatformPrefs.vibrateStateFromId(selected[0]),
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun onEditName(state: ProfileDetailUiState) {
        if (state.isMultiUser) {
            startActivity(GroupCreateActivity.intentEdit(this, state.chatId))
            return
        }
        val contact = dcContext.getContact(state.contactId)
        var authName = contact.authName
        if (TextUtils.isEmpty(authName)) authName = contact.addr
        val view = layoutInflater.inflate(R.layout.single_line_input, null)
        val input = view.findViewById<EditText>(R.id.input_field)
        input.setText(contact.name)
        input.setSelection(input.text.length)
        input.hint = getString(R.string.edit_name_placeholder, authName)
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_edit_name)
            .setMessage(getString(R.string.edit_name_explain, authName))
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    rpc.changeContactName(dcContext.accountId, state.contactId, input.text.toString())
                    reload()
                } catch (e: RpcException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setCancelable(false)
            .show()
    }

    private fun onShare(contactId: Int) {
        val composeIntent = Intent()
        val contact = dcContext.getContact(contactId)
        if (contact.isKeyContact) {
            try {
                val vcard = rpc.makeVcard(rpc.selectedAccountId, Collections.singletonList(contactId)).toByteArray()
                val uri = PlatformMedia.createPersistentBlob(this, vcard, "text/vcard", "contact.vcf")
                PlatformShare.setSharedUris(composeIntent, arrayListOf(uri))
            } catch (_: RpcException) {
                return
            }
        } else {
            PlatformShare.setSharedText(composeIntent, contact.addr)
        }
        HomeRelayingActivity.start(this, composeIntent)
    }

    private fun onEncrInfo(state: ProfileDetailUiState) {
        val info =
            if (state.isContactProfile) {
                dcContext.getContactEncrInfo(state.contactId)
            } else {
                dcContext.getChatEncrInfo(state.chatId)
            }
        val dialog =
            AlertDialog.Builder(this)
                .setMessage(info)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        dialog.findViewById<android.widget.TextView>(android.R.id.message)?.setTextIsSelectable(true)
    }

    private fun onBlockContact(contactId: Int, blocked: Boolean) {
        if (blocked) {
            AlertDialog.Builder(this)
                .setMessage(R.string.ask_unblock_contact)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.menu_unblock_contact) { _, _ ->
                    dcContext.blockContact(contactId, 0)
                    reload()
                }
                .show()
        } else {
            val dialog =
                AlertDialog.Builder(this)
                    .setMessage(R.string.ask_block_contact)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.menu_block_contact) { _, _ ->
                        dcContext.blockContact(contactId, 1)
                        reload()
                    }
                    .show()
            PlatformLegacyUtil.redPositiveButton(dialog)
        }
    }

    companion object {
        const val CHAT_ID_EXTRA = "chat_id"
        const val CONTACT_ID_EXTRA = "contact_id"

        @JvmStatic
        fun intentContact(context: Context, contactId: Int): Intent =
            Intent(context, ProfileDetailActivity::class.java).apply {
                putExtra(CONTACT_ID_EXTRA, contactId)
            }

        @JvmStatic
        fun intentChat(context: Context, chatId: Int): Intent =
            Intent(context, ProfileDetailActivity::class.java).apply {
                putExtra(CHAT_ID_EXTRA, chatId)
            }
    }
}
