package com.polli.android.stories

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b44t.messenger.DcMsg
import com.polli.android.BaseComposeActivity
import com.polli.android.chat.DcMsgMediaContent
import com.polli.android.chat.ChatComposerDock
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.theme.LabTheme
import com.polli.android.ui.LabAvatar
import com.polli.android.ui.AppInsets
import kotlinx.coroutines.delay
import org.thoughtcrime.securesms.connect.DcHelper
import kotlin.math.roundToInt

private const val STORY_SEGMENT_MS = 5_500L
private const val SWIPE_CLOSE_THRESHOLD_PX = 120f

class ChannelStoriesActivity : BaseComposeActivity() {
    private val storiesViewModel: StoriesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val channelIds = intent.getIntArrayExtra(EXTRA_CHANNEL_IDS)?.toList() ?: emptyList()
        val startId = intent.getIntExtra(EXTRA_CHANNEL_ID, -1)
        val startIdx = channelIds.indexOf(startId).coerceAtLeast(0)
        val prefs = AppPrefs(this)
        setContent {
            LabTheme(prefs = prefs) {
                ChannelStoriesScreen(
                    channelIds = channelIds,
                    startIndex = startIdx,
                    storiesViewModel = storiesViewModel,
                    onClose = { finish() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        storiesViewModel.reload()
    }

    companion object {
        private const val EXTRA_CHANNEL_ID = "channel_id"
        private const val EXTRA_CHANNEL_IDS = "channel_ids"

        fun intent(context: Context, channelId: Int, allChannelIds: List<Int>): Intent {
            return Intent(context, ChannelStoriesActivity::class.java).apply {
                putExtra(EXTRA_CHANNEL_ID, channelId)
                putExtra(EXTRA_CHANNEL_IDS, allChannelIds.toIntArray())
            }
        }
    }
}

@Composable
private fun ChannelStoriesScreen(
    channelIds: List<Int>,
    startIndex: Int,
    storiesViewModel: StoriesViewModel,
    onClose: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dc = remember { DcHelper.getContext(context) }
    var channelIdx by remember { mutableIntStateOf(startIndex.coerceIn(0, (channelIds.size - 1).coerceAtLeast(0))) }
    var postIdx by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var segmentStart by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var replyDraft by remember { mutableStateOf("") }
    var animTick by remember { mutableIntStateOf(0) }

    val chatId = channelIds.getOrNull(channelIdx) ?: return
    val chat = remember(chatId) { dc.getChat(chatId) }

    LaunchedEffect(chatId) {
        storiesViewModel.bind(chatId)
        postIdx = 0
        segmentStart = System.currentTimeMillis()
    }

    val posts = storiesViewModel.posts
    val postCount = posts.size.coerceAtLeast(1)
    val safePostIdx = if (posts.isEmpty()) 0 else postIdx.coerceIn(0, posts.lastIndex)
    val post: DcMsg? = posts.getOrNull(safePostIdx)

    LaunchedEffect(Unit) {
        while (true) {
            delay(40)
            animTick++
        }
    }

    val segmentProgress = remember(animTick, segmentStart, paused, posts.size) {
        if (paused || posts.isEmpty()) {
            0f
        } else {
            ((System.currentTimeMillis() - segmentStart).toFloat() / STORY_SEGMENT_MS).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(channelIdx, safePostIdx, paused, posts.size) {
        segmentStart = System.currentTimeMillis()
        if (paused || posts.isEmpty()) return@LaunchedEffect
        delay(STORY_SEGMENT_MS)
        if (safePostIdx + 1 < posts.size) {
            postIdx = safePostIdx + 1
        } else if (channelIdx + 1 < channelIds.size) {
            channelIdx += 1
            postIdx = 0
        } else {
            onClose()
        }
    }

    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.Black)
            .offset { IntOffset(0, dragY.coerceAtLeast(0f).roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragY > SWIPE_CLOSE_THRESHOLD_PX) {
                            onClose()
                        }
                        dragY = 0f
                    },
                    onVerticalDrag = { _, delta ->
                        dragY = (dragY + delta).coerceAtLeast(0f)
                    },
                )
            },
    ) {
        StoryContentSlide(post = post)

        // Tap zones — isolated from pause; left = previous, right = next
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(120.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    if (safePostIdx > 0) postIdx = safePostIdx - 1
                    else if (channelIdx > 0) {
                        channelIdx -= 1
                        postIdx = 0
                    }
                },
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(120.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    if (safePostIdx + 1 < posts.size) postIdx = safePostIdx + 1
                    else if (channelIdx + 1 < channelIds.size) {
                        channelIdx += 1
                        postIdx = 0
                    } else {
                        onClose()
                    }
                },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to LabColors.Black.copy(alpha = 0.92f),
                            0.55f to LabColors.Black.copy(alpha = 0.72f),
                            1f to Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 12.dp)
                .padding(top = maxOf(10.dp, AppInsets.statusBarTop()))
                .padding(bottom = 14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(postCount) { i ->
                    val fill = when {
                        i < safePostIdx -> 1f
                        i == safePostIdx -> segmentProgress
                        else -> 0f
                    }
                    StoryProgressSegment(
                        fraction = fill,
                        modifier = Modifier
                            .weight(1f)
                            .height(LabDimens.StoryProgressHeight),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LabAvatar(name = chat.name, seed = chat.name, size = 32.dp, chatId = chatId)
                Row(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(
                        chat.name,
                        color = LabColors.White85,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            ChatComposerDock(
                value = replyDraft,
                onValueChange = { replyDraft = it },
                onSend = {
                    val text = replyDraft.trim()
                    if (text.isEmpty()) return@ChatComposerDock
                    val msg = DcMsg(dc, DcMsg.DC_MSG_TEXT)
                    msg.setText(text)
                    post?.let { quoted ->
                        if (quoted.isOk) msg.setQuote(quoted)
                    }
                    dc.sendMsg(chatId, msg)
                    replyDraft = ""
                    storiesViewModel.reload()
                },
            )
        }
    }
}

@Composable
private fun StoryProgressSegment(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(1.dp))
            .background(LabColors.White16),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(1.dp))
                    .background(LabColors.White),
            )
        }
    }
}

@Composable
private fun StoryContentSlide(post: DcMsg?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        LabColors.Blurple.copy(alpha = 0.2f),
                        LabColors.Black.copy(alpha = 0.75f),
                        LabColors.Black,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (post == null) {
            Text(
                "No posts in this channel yet.",
                color = LabColors.White33,
                fontSize = 15.sp,
            )
            return
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (post.hasFile()) {
                DcMsgMediaContent(msg = post)
            }
            post.text?.trim()?.takeIf { it.isNotBlank() }?.let { body ->
                Text(
                    text = body,
                    color = LabColors.White85,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 24,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
