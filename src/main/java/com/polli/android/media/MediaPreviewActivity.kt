package com.polli.android.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import com.b44t.messenger.DcMsg
import com.bumptech.glide.Glide
import com.polli.android.BaseComposeActivity
import com.polli.android.icons.PolliIcon
import com.polli.android.icons.PolliIconName
import com.polli.android.newchat.GroupCreateActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.theme.accent
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import com.polli.android.platform.EngineBridge
import com.polli.android.mms.Slide
import com.polli.android.util.SaveAttachmentTask
import com.polli.android.util.StorageUtil
import java.io.File

/** Compose media preview — replaces Java MediaPreviewActivity. */
class MediaPreviewActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val msgId = intent.getIntExtra(DC_MSG_ID, DcMsg.DC_MSG_NO_ID)
        val avatarUri = intent.data
        val title = intent.getStringExtra(ACTIVITY_TITLE_EXTRA)
        val editChatId = intent.getIntExtra(EDIT_AVATAR_CHAT_ID, 0)
        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                when {
                    msgId != DcMsg.DC_MSG_NO_ID -> {
                        MediaPreviewScreen(
                            msgId = msgId,
                            onBack = { finish() },
                            onOpenAllMedia = { chatId ->
                                startActivity(ChatAllMediaActivity.intent(this, chatId))
                                finish()
                            },
                        )
                    }
                    avatarUri != null -> {
                        AvatarPreviewScreen(
                            uri = avatarUri,
                            title = title.orEmpty(),
                            editChatId = editChatId,
                            onBack = { finish() },
                            onEditAvatar = {
                                startActivity(GroupCreateActivity.intentEdit(this, editChatId))
                                finish()
                            },
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(PolliColors.Black),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Media unavailable", color = PolliColors.White33)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val DC_MSG_ID = "dc_msg_id"
        const val ACTIVITY_TITLE_EXTRA = "activity_title"
        const val EDIT_AVATAR_CHAT_ID = "avatar_for_chat_id"
        const val OUTGOING_EXTRA = "outgoing"

        @JvmStatic
        fun intent(context: Context, messageId: Int): Intent =
            Intent(context, MediaPreviewActivity::class.java).apply {
                putExtra(DC_MSG_ID, messageId)
            }

        @JvmStatic
        fun intentAvatar(
            context: Context,
            file: File,
            mimeType: String,
            title: String,
            editChatId: Int = 0,
        ): Intent =
            Intent(context, MediaPreviewActivity::class.java).apply {
                setDataAndType(Uri.fromFile(file), mimeType)
                putExtra(ACTIVITY_TITLE_EXTRA, title)
                if (editChatId != 0) {
                    putExtra(EDIT_AVATAR_CHAT_ID, editChatId)
                }
            }

        @JvmStatic
        fun isTypeSupported(slide: Slide?): Boolean =
            slide != null && (slide.hasVideo() || slide.hasImage())
    }
}

@Composable
fun MediaPreviewScreen(
    msgId: Int,
    onBack: () -> Unit,
    onOpenAllMedia: (Int) -> Unit,
) {
    val context = LocalContext.current
    val gallery = remember(msgId) { MediaGalleryLoad.galleryForMessage(context, msgId) }
    if (gallery == null || gallery.messageIds.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PolliColors.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text("Media unavailable", color = PolliColors.White33)
        }
        return
    }

    val msgIds = gallery.messageIds
    val pagerState = rememberPagerState(
        initialPage = gallery.initialIndex.coerceIn(0, msgIds.lastIndex),
        pageCount = { msgIds.size },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppInsets.statusBarTop() + 8.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBackButton(onClick = onBack)
            Text(
                text = "${pagerState.currentPage + 1} / ${msgIds.size}",
                color = PolliColors.White85,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            IconButton(onClick = {
                val id = msgIds[pagerState.currentPage]
                EngineBridge.openForViewOrShare(context, id, Intent.ACTION_SEND)
            }) {
                PolliIcon(PolliIconName.Options, 22.dp, PolliColors.White85)
            }
            IconButton(onClick = {
                val id = msgIds[pagerState.currentPage]
                saveMessageToDisk(context as Activity, id)
            }) {
                PolliIcon(PolliIconName.ArrowDown, 22.dp, PolliColors.White85)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        ) { page ->
            MediaPage(msgIds[page])
        }
    }
}

@Composable
private fun AvatarPreviewScreen(
    uri: Uri,
    title: String,
    editChatId: Int,
    onBack: () -> Unit,
    onEditAvatar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppInsets.statusBarTop() + 8.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBackButton(onClick = onBack)
            Text(
                text = title,
                color = PolliColors.White85,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            if (editChatId != 0) {
                TextButton(onClick = onEditAvatar) {
                    Text("Edit", color = accent().light)
                }
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
            },
            update = { view ->
                Glide.with(view).load(uri).fitCenter().into(view)
            },
        )
    }
}

@Composable
private fun MediaPage(msgId: Int) {
    val context = LocalContext.current
    val msg = remember(msgId) { EngineBridge.getContext(context).getMsg(msgId) }

    if (!msg.isOk || !msg.hasFile()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Media unavailable", color = PolliColors.White33)
        }
        return
    }

    val file = remember(msgId) { msg.getFileAsFile() }
    val isVideo = remember(msgId) { msg.filemime?.startsWith("video/") == true }

    if (isVideo) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { EngineBridge.openForViewOrShare(context, msgId, Intent.ACTION_VIEW) },
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
                },
                update = { view ->
                    Glide.with(view).asBitmap().load(file).frame(1_000_000).fitCenter().into(view)
                },
            )
            Text("▶ Tap to play", color = PolliColors.White85)
        }
    } else {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
            },
            update = { view ->
                Glide.with(view).load(file).fitCenter().into(view)
            },
        )
    }
}

private fun saveMessageToDisk(activity: Activity, msgId: Int) {
    if (!StorageUtil.canWriteToMediaStore(activity)) {
        Toast.makeText(activity, com.polli.android.R.string.error, Toast.LENGTH_SHORT).show()
        return
    }
    SaveAttachmentTask.showWarningDialog(activity) { _, _ ->
        val dc = EngineBridge.getContext(activity)
        val msg = dc.getMsg(msgId)
        if (!msg.isOk || !msg.hasFile()) return@showWarningDialog
        SaveAttachmentTask(activity).executeOnExecutor(
            android.os.AsyncTask.THREAD_POOL_EXECUTOR,
            SaveAttachmentTask.Attachment(
                Uri.fromFile(msg.getFileAsFile()),
                msg.filemime,
                msg.dateReceived.coerceAtLeast(System.currentTimeMillis()),
                msg.filename,
            ),
        )
    }
}
