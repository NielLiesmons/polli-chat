package com.polli.android.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import com.polli.android.data.engine.PolliRepositories
import com.polli.core.chat.ChatMediaFilter
import com.bumptech.glide.Glide
import com.polli.android.BaseComposeActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import com.polli.domain.navigation.ChatIntentExtras
import com.polli.ui.components.ChatMediaBrowser
import com.polli.android.platform.EngineBridge

class ChatAllMediaActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chatId = intent.getIntExtra(ChatIntentExtras.CHAT_ID, -1)
        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                ChatAllMediaScreen(
                    chatId = chatId,
                    onBack = { finish() },
                    onOpenMessage = { msgId ->
                        startActivity(MediaPreviewActivity.intent(this, msgId))
                    },
                )
            }
        }
    }

    companion object {
        fun intent(context: Context, chatId: Int): Intent =
            Intent(context, ChatAllMediaActivity::class.java).apply {
                putExtra(ChatIntentExtras.CHAT_ID, chatId)
            }
    }
}

@Composable
fun ChatMediaTabPanel(
    chatId: Int,
    topPadding: androidx.compose.ui.unit.Dp,
    onOpenMessage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val filter = ChatMediaFilter.entries[selectedTab]
    val context = LocalContext.current
    val mediaRepo = remember { PolliRepositories.media(context) }
    val msgIds = remember(chatId, filter) {
        mediaRepo.messageIdsForFilter(chatId, filter)
    }

    ChatMediaBrowser(
        messageIds = msgIds,
        selectedFilterIndex = selectedTab,
        onFilterSelected = { selectedTab = it },
        modifier = modifier,
        topPadding = topPadding,
        gridCell = { msgId, cellModifier ->
            GalleryThumb(
                msgId = msgId,
                onClick = { onOpenMessage(msgId) },
                modifier = cellModifier,
            )
        },
        listRow = { msgId, rowModifier ->
            MediaListRow(
                msgId = msgId,
                onClick = { onOpenMessage(msgId) },
                modifier = rowModifier,
            )
        },
    )
}

@Composable
fun ChatAllMediaScreen(
    chatId: Int,
    onBack: () -> Unit,
    onOpenMessage: (Int) -> Unit,
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
                "All media",
                color = PolliColors.White85,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        ChatMediaTabPanel(
            chatId = chatId,
            topPadding = 0.dp,
            onOpenMessage = onOpenMessage,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun GalleryThumb(msgId: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val msg = remember(msgId) { EngineBridge.getContext(context).getMsg(msgId) }
    val file = remember(msgId) { if (msg.isOk && msg.hasFile()) msg.getFileAsFile() else null }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(PolliColors.Gray33)
            .clickable(onClick = onClick),
    ) {
        if (file != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                },
                update = { view ->
                    Glide.with(view).load(file).centerCrop().into(view)
                },
            )
        }
    }
}

@Composable
fun MediaListRow(msgId: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val msg = remember(msgId) { EngineBridge.getContext(context).getMsg(msgId) }
    val label = remember(msgId) {
        when {
            !msg.isOk -> "Message"
            msg.filename?.isNotBlank() == true -> msg.filename
            msg.text?.isNotBlank() == true -> msg.text
            else -> msg.filemime ?: "Attachment"
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PolliColors.Gray33)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PolliColors.White85, modifier = Modifier.weight(1f))
    }
}
