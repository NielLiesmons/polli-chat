package com.polli.android.stories

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.b44t.messenger.DcMsg
import com.polli.android.BaseComposeActivity
import com.polli.android.R
import com.polli.android.chat.ChatComposerDock
import com.polli.android.chat.DcMsgMediaContent
import com.polli.android.icons.PolliIcon
import com.polli.android.icons.PolliIconName
import com.polli.android.platform.EngineBridge
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliDimens
import com.polli.android.theme.PolliTheme
import com.polli.android.theme.accent
import com.polli.android.ui.AppInsets
import com.polli.android.ui.ChatFeedEdgeGradients
import com.polli.android.ui.PolliAvatar
import com.polli.android.ui.rememberComposerChromeLayout
import com.polli.android.ui.rememberPolliHazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val STORY_SEGMENT_MS = 5_500L

class ChannelStoriesActivity : BaseComposeActivity() {
    private val storiesViewModel: StoriesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val channelIds = intent.getIntArrayExtra(EXTRA_CHANNEL_IDS)?.toList() ?: emptyList()
        val startId = intent.getIntExtra(EXTRA_CHANNEL_ID, -1)
        val startIdx = channelIds.indexOf(startId).coerceAtLeast(0)
        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                ChannelStoriesOverlay(
                    session = StorySession(
                        channelId = startId,
                        channelIds = channelIds,
                        launchBounds = StoryLaunchBounds(0f, 0f, 0f),
                    ),
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
fun ChannelStoriesScreen(
    channelIds: List<Int>,
    startIndex: Int,
    storiesViewModel: StoriesViewModel,
    onClose: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dc = remember { EngineBridge.getContext(context) }
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, (channelIds.size - 1).coerceAtLeast(0)),
        pageCount = { channelIds.size.coerceAtLeast(1) },
    )
    var postIdx by remember { mutableIntStateOf(0) }
    var replyDraft by remember { mutableStateOf("") }
    var composerFocused by remember { mutableStateOf(false) }
    var userPaused by remember { mutableStateOf(false) }
    var postsReadyForChat by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    val channelIdx = pagerState.currentPage
    val chatId = channelIds.getOrNull(channelIdx) ?: return
    val chat = remember(chatId) { dc.getChat(chatId) }

    LaunchedEffect(chatId) {
        storiesViewModel.bind(chatId)
        replyDraft = ""
        composerFocused = false
        userPaused = false
        postsReadyForChat = -1
    }

    val posts = storiesViewModel.posts
    LaunchedEffect(chatId, posts) {
        if (postsReadyForChat != chatId) {
            postIdx = ChannelStoryRingLogic.startPostIndex(posts)
            postsReadyForChat = chatId
        }
    }

    val postCount = posts.size.coerceAtLeast(1)
    val safePostIdx = if (posts.isEmpty()) 0 else postIdx.coerceIn(0, posts.lastIndex)
    val post: DcMsg? = posts.getOrNull(safePostIdx)

    LaunchedEffect(chatId, safePostIdx, posts.size) {
        if (posts.isNotEmpty()) {
            dc.marknoticedChat(chatId)
        }
    }

    val progressKey = channelIdx to safePostIdx
    var segmentElapsedMs by remember(progressKey) { mutableFloatStateOf(0f) }
    val canReplyPrivately = ChannelStoryReply.canReply(dc, chat, post)
    val paused = userPaused || composerFocused || replyDraft.isNotEmpty()
    val hazeState = rememberPolliHazeState()
    val composerChrome = rememberComposerChromeLayout()

    LaunchedEffect(progressKey, paused, posts.size) {
        if (posts.isEmpty()) return@LaunchedEffect
        while (segmentElapsedMs < STORY_SEGMENT_MS) {
            if (paused) {
                delay(40)
                continue
            }
            delay(40)
            segmentElapsedMs = (segmentElapsedMs + 40f).coerceAtMost(STORY_SEGMENT_MS.toFloat())
        }
        if (safePostIdx + 1 < posts.size) {
            postIdx = safePostIdx + 1
        } else if (channelIdx + 1 < channelIds.size) {
            pagerState.animateScrollToPage(channelIdx + 1)
        } else {
            onClose()
        }
    }

    val segmentProgress =
        if (posts.isEmpty()) {
            0f
        } else {
            (segmentElapsedMs / STORY_SEGMENT_MS.toFloat()).coerceIn(0f, 1f)
        }

    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .onGloballyPositioned { composerChrome.onRootPositioned(it) },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { channelIds.getOrElse(it) { it } },
        ) { page ->
            // Only the active page binds posts via ViewModel; others show a placeholder slide.
            if (page == channelIdx) {
                StoryContentSlide(
                    post = post,
                    bottomPadding = if (canReplyPrivately) composerChrome.feedBottomPadding else 0.dp,
                    modifier = Modifier.hazeSource(state = hazeState),
                )
            } else {
                StoryContentSlide(
                    post = null,
                    bottomPadding = 0.dp,
                )
            }
        }

        // Tap zones — left/right within the current profile's stories
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(96.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    if (safePostIdx > 0) {
                        postIdx = safePostIdx - 1
                    } else if (channelIdx > 0) {
                        scope.launch { pagerState.animateScrollToPage(channelIdx - 1) }
                    }
                },
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(96.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    if (safePostIdx + 1 < posts.size) {
                        postIdx = safePostIdx + 1
                    } else if (channelIdx + 1 < channelIds.size) {
                        scope.launch { pagerState.animateScrollToPage(channelIdx + 1) }
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
                            0f to PolliColors.Black.copy(alpha = 0.92f),
                            0.55f to PolliColors.Black.copy(alpha = 0.72f),
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
                            .height(PolliDimens.StoryProgressHeight),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PolliAvatar(name = chat.name, seed = chat.name, size = 32.dp, chatId = chatId)
                Text(
                    chat.name,
                    color = PolliColors.White85,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f),
                )
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { userPaused = !userPaused }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PolliIcon(
                        if (userPaused) PolliIconName.Play else PolliIconName.Pause,
                        18.dp,
                        PolliColors.White85,
                    )
                }
            }
        }

        if (canReplyPrivately) {
            ChatFeedEdgeGradients(
                modifier = Modifier.fillMaxSize(),
                bottomChromeInset = composerChrome.bottomChromeInset,
            )

            ChatComposerDock(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .onGloballyPositioned { composerChrome.onComposerPositioned(it) },
                value = replyDraft,
                onValueChange = { replyDraft = it },
                onFocusChanged = { composerFocused = it },
                hazeState = hazeState,
                onSend = {
                    val text = replyDraft.trim()
                    if (text.isEmpty()) return@ChatComposerDock
                    val sent = ChannelStoryReply.sendPrivateReply(dc, chat, post, text)
                    if (sent) {
                        replyDraft = ""
                        Toast.makeText(
                            context,
                            R.string.last_msg_sent_successfully,
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        Toast.makeText(context, R.string.error, Toast.LENGTH_SHORT).show()
                    }
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
            .background(PolliColors.White16),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(1.dp))
                    .background(PolliColors.White),
            )
        }
    }
}

@Composable
private fun StoryContentSlide(
    post: DcMsg?,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val contentBottomInset = bottomPadding + 24.dp
    val contentTopInset = 108.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent().solid.copy(alpha = 0.2f),
                        PolliColors.Black.copy(alpha = 0.75f),
                        PolliColors.Black,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentTopInset, bottom = contentBottomInset),
            contentAlignment = Alignment.Center,
        ) {
            if (post == null) {
                Text(
                    "No posts in this channel yet.",
                    color = PolliColors.White33,
                    fontSize = 15.sp,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (post.hasFile()) {
                        DcMsgMediaContent(msg = post)
                    }
                    post.text?.trim()?.takeIf { it.isNotBlank() }?.let { body ->
                        Text(
                            text = body,
                            color = PolliColors.White85,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 24,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
