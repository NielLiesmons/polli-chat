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
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import org.thoughtcrime.securesms.connect.DcHelper
import org.thoughtcrime.securesms.util.SaveAttachmentTask
import org.thoughtcrime.securesms.util.StorageUtil

class MediaPreviewActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val msgId = intent.getIntExtra(DC_MSG_ID, DcMsg.DC_MSG_NO_ID)
        val prefs = AppPrefs(this)
        setContent {
            LabTheme(prefs = prefs) {
                MediaPreviewScreen(
                    msgId = msgId,
                    onBack = { finish() },
                    onOpenAllMedia = { chatId ->
                        startActivity(ChatAllMediaActivity.intent(this, chatId))
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val DC_MSG_ID = "dc_msg_id"

        fun intent(context: Context, messageId: Int): Intent =
            Intent(context, MediaPreviewActivity::class.java).apply {
                putExtra(DC_MSG_ID, messageId)
            }
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
                .background(LabColors.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text("Media unavailable", color = LabColors.White33)
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
            .background(LabColors.Black),
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
                color = LabColors.White85,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            IconButton(onClick = {
                val id = msgIds[pagerState.currentPage]
                DcHelper.openForViewOrShare(context, id, Intent.ACTION_SEND)
            }) {
                LabIcon(LabIconName.Options, 22.dp, LabColors.White85)
            }
            IconButton(onClick = {
                val id = msgIds[pagerState.currentPage]
                saveMessageToDisk(context as Activity, id)
            }) {
                LabIcon(LabIconName.ArrowDown, 22.dp, LabColors.White85)
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
private fun MediaPage(msgId: Int) {
    val context = LocalContext.current
    val msg = remember(msgId) { DcHelper.getContext(context).getMsg(msgId) }

    if (!msg.isOk || !msg.hasFile()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Media unavailable", color = LabColors.White33)
        }
        return
    }

    val file = remember(msgId) { msg.getFileAsFile() }
    val isVideo = remember(msgId) { msg.filemime?.startsWith("video/") == true }

    if (isVideo) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { DcHelper.openForViewOrShare(context, msgId, Intent.ACTION_VIEW) },
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
            Text("▶ Tap to play", color = LabColors.White85)
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
        Toast.makeText(activity, org.thoughtcrime.securesms.R.string.error, Toast.LENGTH_SHORT).show()
        return
    }
    SaveAttachmentTask.showWarningDialog(activity) { _, _ ->
        val dc = DcHelper.getContext(activity)
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
