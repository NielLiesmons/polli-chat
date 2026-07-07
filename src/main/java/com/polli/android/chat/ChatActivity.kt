package com.polli.android.chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import com.polli.android.media.MediaEditLauncher
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliTheme
import com.polli.android.data.engine.PolliRepositories
import com.polli.domain.navigation.ChatIntentExtras
import com.polli.android.platform.EngineBridge
import com.polli.android.platform.LegacyAttachContactActivity
import com.polli.android.platform.PlatformAttachments
import com.polli.android.platform.PlatformMedia
import com.polli.android.platform.PolliAudioPlaybackService
import com.polli.android.platform.PolliAudioPlaybackViewModel
import com.polli.android.platform.PolliChatAudioQueueProvider
import org.thoughtcrime.securesms.R
import chat.delta.rpc.RpcException
import java.io.File
import java.util.Collections

/** Single Compose chat host — replaces ConversationActivity when POLLI_UI is enabled. */
class ChatActivity : BaseComposeActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private val playbackViewModel: PolliAudioPlaybackViewModel by viewModels()
    private var themeRevision by mutableIntStateOf(0)
    private var showAttachModal by mutableStateOf(false)
    private var pendingAttachment by mutableStateOf<PendingAttachment?>(null)
    private var cameraUri: Uri? = null
    private var chatId: Int = -1
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null

    private val imageEditor = MediaEditLauncher(
        activity = this,
        onEdited = { uri -> stageAttachment(uri) },
    )

    private val pickGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) sendPickedUri(uri)
    }

    private val pickDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) sendPickedUri(uri)
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        cameraUri = null
        if (success && uri != null) {
            openImageEditorOrSend(uri)
        }
    }

    private val pickContact = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK || result.data == null) return@registerForActivityResult
        val contactId = result.data!!.getIntExtra(LegacyAttachContactActivity.CONTACT_ID_EXTRA, 0)
        if (contactId <= 0) return@registerForActivityResult
        try {
            val rpc = EngineBridge.getRpc(this)
            val vcard = rpc.makeVcard(rpc.selectedAccountId, Collections.singletonList(contactId)).toByteArray()
            val uri = PlatformMedia.createPersistentBlob(this, vcard, "application/octet-stream", "vcard.vcf")
            sendMedia(uri)
        } catch (_: RpcException) {
            // Ignore — same as ConversationActivity on RPC failure.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatId = intent.getIntExtra(ChatIntentExtras.CHAT_ID, -1)
        if (chatId <= 0) {
            finish()
            return
        }
        val sessionInfo = PolliRepositories.chat(this).getSession(chatId)
        if (sessionInfo == null) {
            finish()
            return
        }
        val draftText = intent.getStringExtra(ChatIntentExtras.DRAFT_TEXT)
        val startingPosition = intent.getIntExtra(ChatIntentExtras.STARTING_POSITION, -1)
        val fromArchived = intent.getBooleanExtra(ChatIntentExtras.FROM_ARCHIVED, false)
        val prefs = AppPrefs(this)

        playbackViewModel.setQueueProvider(
            PolliChatAudioQueueProvider(
                this,
                chatId,
                PolliRepositories.accounts(this).selectedAccountId,
            ),
        )
        initializeMediaController()

        viewModel.bind(
            chatId = chatId,
            initialDraft = draftText,
            startingPosition = startingPosition,
            fromArchived = fromArchived,
        )

        ShareInbound.apply(
            activity = this,
            chatId = chatId,
            viewModel = viewModel,
            stageAttachment = { uri, mime -> stageAttachment(uri, mime) },
        )

        val session = sessionInfo.toActionContext()

        setContent {
            val revision = themeRevision
            PolliTheme(prefs = prefs, uiScaleRevision = revision) {
                CompositionLocalProvider(LocalChatAudioPlayback provides playbackViewModel) {
                    ChatScreen(
                        viewModel = viewModel,
                        chatTitle = sessionInfo.name,
                        chatSeed = sessionInfo.name.ifBlank { chatId.toString() },
                        chatId = chatId,
                        chatSession = session,
                        isGroup = sessionInfo.isMultiUser,
                        isBroadcast = sessionInfo.isBroadcast,
                        uiScaleRevision = revision,
                        playbackViewModel = playbackViewModel,
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
                            pickContact.launch(Intent(this, LegacyAttachContactActivity::class.java))
                        },
                        onPickLocation = {
                            showAttachModal = false
                            PlatformAttachments.selectLocation(this, chatId)
                        },
                        onVoiceSent = { uri, _ ->
                            MediaSend.sendVoice(this, chatId, uri, 0)
                            viewModel.notifyOutboundSent()
                        },
                        pendingAttachment = pendingAttachment,
                        onClearAttachment = { pendingAttachment = null },
                        onSendMessage = { sendComposedMessage() },
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
            android.content.ComponentName(this, PolliAudioPlaybackService::class.java),
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

    private fun sendPickedUri(uri: Uri) {
        val mime = PlatformMedia.mimeType(this, uri) ?: "application/octet-stream"
        when {
            PlatformMedia.isGif(mime) -> stageAttachment(uri, mime)
            PlatformMedia.isVideoType(mime) -> imageEditor.launchVideo(uri)
            PlatformMedia.isImageType(mime) -> openImageEditorOrSend(uri)
            else -> stageAttachment(uri, mime)
        }
    }

    private fun openImageEditorOrSend(uri: Uri) {
        imageEditor.launchImage(uri)
    }

    private fun stageAttachment(uri: Uri, mimeType: String? = null) {
        val mime = mimeType ?: PlatformMedia.mimeType(this, uri) ?: "application/octet-stream"
        pendingAttachment = PendingAttachment(
            uri = uri,
            mimeType = mime,
            label = attachmentLabel(uri),
            isImage = PlatformMedia.isImageType(mime) || PlatformMedia.isGif(mime),
        )
    }

    private fun attachmentLabel(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                cursor.getString(idx)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Attachment"
    }

    private fun sendComposedMessage() {
        val attachment = pendingAttachment
        if (attachment != null) {
            MediaSend.sendUri(this, chatId, attachment.uri, attachment.mimeType, viewModel.draft)
            pendingAttachment = null
            viewModel.clearAfterSend()
            return
        }
        viewModel.send()
    }

    private fun sendMedia(uri: Uri, mimeType: String? = null) {
        if (chatId <= 0) return
        MediaSend.sendUri(this, chatId, uri, mimeType ?: PlatformMedia.mimeType(this, uri))
        viewModel.notifyOutboundSent()
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
                putExtra(ChatIntentExtras.CHAT_ID, chatId)
            }
        }
    }
}
