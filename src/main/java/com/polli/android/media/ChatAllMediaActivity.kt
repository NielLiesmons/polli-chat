package com.polli.android.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import com.polli.android.data.engine.PolliRepositories
import com.polli.core.chat.ChatMediaFilter
import com.bumptech.glide.Glide
import com.polli.android.BaseComposeActivity
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import com.polli.domain.navigation.ChatIntentExtras
import org.thoughtcrime.securesms.connect.DcHelper

class ChatAllMediaActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chatId = intent.getIntExtra(ChatIntentExtras.CHAT_ID, -1)
        val prefs = AppPrefs(this)
        setContent {
            LabTheme(prefs = prefs) {
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = topPadding),
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = LabColors.Black,
            contentColor = LabColors.White85,
            edgePadding = 16.dp,
        ) {
            ChatMediaFilter.entries.forEachIndexed { index, item ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(item.label) },
                )
            }
        }

        if (msgIds.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("No ${filter.label.lowercase()}", color = LabColors.White33)
            }
        } else if (filter.isGrid) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(108.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(msgIds.toList()) { id ->
                    GalleryThumb(msgId = id, onClick = { onOpenMessage(id) })
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(msgIds.toList()) { id ->
                    MediaListRow(msgId = id, onClick = { onOpenMessage(id) })
                }
            }
        }
    }
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
                "All media",
                color = LabColors.White85,
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
private fun GalleryThumb(msgId: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val msg = remember(msgId) { DcHelper.getContext(context).getMsg(msgId) }
    val file = remember(msgId) { if (msg.isOk && msg.hasFile()) msg.getFileAsFile() else null }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(LabColors.Gray33)
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
private fun MediaListRow(msgId: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val msg = remember(msgId) { DcHelper.getContext(context).getMsg(msgId) }
    val label = remember(msgId) {
        when {
            !msg.isOk -> "Message"
            msg.filename?.isNotBlank() == true -> msg.filename
            msg.text?.isNotBlank() == true -> msg.text
            else -> msg.filemime ?: "Attachment"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LabColors.Gray33)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = LabColors.White85, modifier = Modifier.weight(1f))
    }
}
