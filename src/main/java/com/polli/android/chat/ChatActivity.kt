package com.polli.android.chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.polli.android.BaseComposeActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabTheme
import org.thoughtcrime.securesms.AttachContactActivity
import org.thoughtcrime.securesms.ConversationActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.audioplay.AudioPlaybackViewModel
import org.thoughtcrime.securesms.components.audioplay.ChatAudioQueueProvider
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.mms.AttachmentManager
import org.thoughtcrime.securesms.providers.PersistentBlobProvider
import org.thoughtcrime.securesms.service.AudioPlaybackService
import org.thoughtcrime.securesms.util.MediaUtil
import chat.delta.rpc.RpcException
import java.io.File
import java.util.Collections

/** Single Compose chat host — replaces ConversationActivity when POLLI_UI is enabled. */
class ChatActivity : BaseComposeActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private val playbackViewModel: AudioPlaybackViewModel by viewModels()
    private var themeRevision by mutableIntStateOf(0)
    private var showAttachModal by mutableStateOf(false)
    private var cameraUri: Uri? = null
    private var chatId: Int = -1
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null

    private val pickGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) sendMedia(uri)
    }

    private val pickDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) sendMedia(uri)
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { sendMedia(it) }
        cameraUri = null
    }

    private val pickContact = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
        val contactId = result.data!!.getIntExtra(AttachContactActivity.CONTACT_ID_EXTRA, 0)
        if (contactId <= 0) return@registerForActivityResult
        try {
            val rpc = DcHelper.getRpc(this)
            val vcard = rpc.makeVcard(rpc.selectedAccountId, Collections.singletonList(contactId)).toByteArray()
            val uri = PersistentBlobProvider.getInstance().create(this, vcard, "application/octet-stream", "vcard.vcf")
            sendMedia(uri)
        } catch (_: RpcException) {
            // Ignore — same as ConversationActivity on RPC failure.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatId = intent.getIntExtra(ConversationActivity.CHAT_ID_EXTRA, -1)
        if (chatId <= 0) {
            finish()
            return
        }
        val dc = DcHelper.getContext(this)
        val chat = dc.getChat(chatId)
        val draftText = intent.getStringExtra(ConversationActivity.TEXT_EXTRA)
        val startingPosition = intent.getIntExtra(ConversationActivity.STARTING_POSITION_EXTRA, -1)
        val fromArchived = intent.getBooleanExtra(ConversationActivity.FROM_ARCHIVED_CHATS_EXTRA, false)
        val prefs = AppPrefs(this)

        playbackViewModel.setQueueProvider(
            ChatAudioQueueProvider(this, chatId, dc.accountId),
        )
        initializeMediaController()

        viewModel.bind(
            chatId = chatId,
            initialDraft = draftText,
            startingPosition = startingPosition,
            fromArchived = fromArchived,
        )

        setContent {
            val revision = themeRevision
            LabTheme(prefs = prefs, uiScaleRevision = revision) {
                CompositionLocalProvider(LocalChatAudioPlayback provides playbackViewModel) {
                    ChatScreen(
                        viewModel = viewModel,
                        chatTitle = chat.name ?: "Chat",
                        chatSeed = chat.name ?: chatId.toString(),
                        chatId = chatId,
                        isGroup = chat.isMultiUser,
                        isBroadcast = chat.isOutBroadcast || chat.isInBroadcast,
                        showAttachModal = showAttachModal,
                        onDismissAttachModal = { showAttachModal = false },
                        onAttachClick = { showAttachModal = true },
                        onBrowseFiles = {
                            showAttachModal = false
                            pickDocument.launch(arrayOf("*/*"))
                        },
                        onCamera = {
                            showAttachModal = false
                            launchCamera()
                        },
                        onPickGallery = {
                            showAttachModal = false
                            pickGallery.launch("image/*")
                        },
                        onPickVideo = {
                            showAttachModal = false
                            pickGallery.launch("video/*")
                        },
                        onPickContact = {
                            showAttachModal = false
                            pickContact.launch(Intent(this, AttachContactActivity::class.java))
                        },
                        onPickLocation = {
                            showAttachModal = false
                            AttachmentManager.selectLocation(this, chatId)
                        },
                        onVoiceSent = { uri, _ ->
                            MediaSend.sendVoice(this, chatId, uri, 0)
                            viewModel.reload()
                        },
                        onBack = { finish() },
                    )
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right)
    }

    override fun onDestroy() {
        mediaControllerFuture?.let { MediaController.releaseFuture(it) }
        mediaControllerFuture = null
        playbackViewModel.setMediaController(null)
        playbackViewModel.setQueueProvider(null)
        super.onDestroy()
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(
            this,
            android.content.ComponentName(this, AudioPlaybackService::class.java),
        )
        mediaControllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        mediaControllerFuture?.addListener(
            {
                try {
                    playbackViewModel.setMediaController(mediaControllerFuture?.get())
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting to audio playback service", e)
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun launchCamera() {
        val file = File.createTempFile("polli_capture", ".jpg", cacheDir)
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        cameraUri = uri
        takePhoto.launch(uri)
    }

    private fun sendMedia(uri: Uri) {
        if (chatId <= 0) return
        MediaSend.sendUri(this, chatId, uri, MediaUtil.getMimeType(this, uri))
        viewModel.reload()
    }

    override fun onResume() {
        super.onResume()
        themeRevision++
    }

    companion object {
        private const val TAG = "ChatActivity"

        @JvmStatic
        fun intent(context: android.content.Context, chatId: Int): Intent {
            return Intent(context, ChatActivity::class.java).apply {
                putExtra(ConversationActivity.CHAT_ID_EXTRA, chatId)
            }
        }
    }
}
