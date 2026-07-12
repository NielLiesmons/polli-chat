package com.polli.android.chat

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.unit.Dp
import androidx.recyclerview.widget.RecyclerView
import com.polli.core.chat.MessageGroupLayout
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.FeedItem
import com.polli.domain.model.chat.displayIndexForMessage
import com.polli.domain.model.chat.stableRowId
import com.polli.android.settings.AppPrefs
import com.polli.ui.components.chat.ChatDayMarkerPill
import com.polli.ui.components.chat.ChatNewMessagesPill
import com.polli.android.R
import com.polli.android.platform.PolliAudioPlaybackViewModel

private data class FeedRowKey(
    val stableId: Long,
    val label: String?,
    val olderMsgId: Int,
    val selfMsgId: Int,
    val newerMsgId: Int,
    val isFirstInStack: Boolean,
    val isLastInStack: Boolean,
)

/**
 * DC [org.thoughtcrime.securesms.ConversationAdapter]: feed rows + lazy message hydrate per bind.
 * Compose rows inherit parent composition; text-only rows can use [PolliTextMessageRowView].
 */
class PolliChatFeedAdapter(
    private val viewModel: ChatViewModel,
    private val prefs: AppPrefs,
    private val uiScaleRevision: Int,
    private val playbackViewModel: PolliAudioPlaybackViewModel?,
    private val maxBubbleWidth: Dp,
    private val onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
    private val onQuoteClick: (Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TEXT = 0
        private const val VIEW_TYPE_COMPOSE = 1
        private const val VIEW_TYPE_MARKER = 2
        const val PAYLOAD_CONTENT = "content"
        const val PAYLOAD_HIGHLIGHT = "highlight"
    }

    private var displayItems: List<FeedItem> = emptyList()
    private var highlightMsgId by mutableIntStateOf(-1)
    private var pulseMsgId by mutableIntStateOf(-1)
    private var pulseEmoji by mutableStateOf<String?>(null)

    init {
        setHasStableIds(true)
    }

    fun updateChrome(highlightId: Int, pulse: com.polli.ui.chat.ReactionPulse?) {
        val nextPulseMsgId = pulse?.msgId ?: -1
        val nextPulseEmoji = pulse?.emoji
        if (highlightMsgId == highlightId && pulseMsgId == nextPulseMsgId && pulseEmoji == nextPulseEmoji) return
        highlightMsgId = highlightId
        pulseMsgId = nextPulseMsgId
        pulseEmoji = nextPulseEmoji
        if (itemCount == 0) return
        notifyItemRangeChanged(0, itemCount, PAYLOAD_HIGHLIGHT)
    }

    fun refreshContent() {
        if (itemCount == 0) return
        notifyItemRangeChanged(0, itemCount, PAYLOAD_CONTENT)
    }

    /** @return true when the feed changed */
    fun changeData(items: List<FeedItem>, structuralReload: Boolean = false): Boolean {
        if (items == displayItems) return false
        val oldSize = displayItems.size
        displayItems = items
        when {
            structuralReload || kotlin.math.abs(items.size - oldSize) > 8 -> notifyDataSetChanged()
            else -> notifyItemRangeChanged(0, itemCount, PAYLOAD_CONTENT)
        }
        return true
    }

    override fun getItemCount(): Int = displayItems.size

    override fun getItemViewType(position: Int): Int {
        return when (chronItem(position)) {
            is FeedItem.DayMarker, FeedItem.NewMessages -> VIEW_TYPE_MARKER
            is FeedItem.Message -> {
                val feedMessage = chronItem(position) as FeedItem.Message
                val stub = viewModel.getStub(feedMessage.msgId)
                if (stub != null && stub.hasText && !stub.hasAttachment && !stub.isInfo) {
                    VIEW_TYPE_TEXT
                } else {
                    VIEW_TYPE_COMPOSE
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= displayItems.size) return RecyclerView.NO_ID
        return chronItem(position).stableRowId()
    }

    fun displayIndexForMsgId(msgId: Int): Int = displayItems.displayIndexForMessage(msgId)

    private fun chronIndex(displayPosition: Int): Int = displayItems.size - 1 - displayPosition

    private fun chronItem(displayPosition: Int): FeedItem = displayItems[chronIndex(displayPosition)]

    private fun rowKey(items: List<FeedItem>, displayPosition: Int): FeedRowKey {
        val item = items[chronIndex(displayPosition)]
        return when (item) {
            is FeedItem.DayMarker ->
                FeedRowKey(
                    stableId = item.stableRowId(),
                    label = item.label,
                    olderMsgId = 0,
                    selfMsgId = 0,
                    newerMsgId = 0,
                    isFirstInStack = true,
                    isLastInStack = true,
                )
            is FeedItem.Message ->
                FeedRowKey(
                    stableId = item.stableRowId(),
                    label = null,
                    olderMsgId = item.olderMsgId ?: 0,
                    selfMsgId = item.msgId,
                    newerMsgId = item.newerMsgId ?: 0,
                    isFirstInStack = item.groupLayout.isFirstInStack,
                    isLastInStack = item.groupLayout.isLastInStack,
                )
            else ->
                FeedRowKey(
                    stableId = item.stableRowId(),
                    label = null,
                    olderMsgId = 0,
                    selfMsgId = 0,
                    newerMsgId = 0,
                    isFirstInStack = true,
                    isLastInStack = true,
                )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TEXT ->
                TextHolder(
                    PolliTextMessageRowView(parent.context).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    },
                )
            VIEW_TYPE_MARKER, VIEW_TYPE_COMPOSE -> {
                val composeView =
                    ComposeView(parent.context).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                        (parent as? View)?.findViewTreeCompositionContext()?.let { setParentCompositionContext(it) }
                    }
                if (viewType == VIEW_TYPE_MARKER) {
                    MarkerHolder(composeView)
                } else {
                    ComposeHolder(
                        composeView = composeView,
                        viewModel = viewModel,
                        playbackViewModel = playbackViewModel,
                        maxBubbleWidth = maxBubbleWidth,
                        onOpenMessageOverlay = onOpenMessageOverlay,
                        onQuoteClick = onQuoteClick,
                    )
                }
            }
            else -> error("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        bindInternal(holder, position, emptyList())
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        bindInternal(holder, position, payloads)
    }

    private fun bindInternal(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        val item = chronItem(position)
        val highlighted = item is FeedItem.Message && item.msgId == highlightMsgId
        val rowPulse = if (item is FeedItem.Message && item.msgId == pulseMsgId) pulseEmoji else null
        when (holder) {
            is TextHolder -> {
                if (item !is FeedItem.Message) return
                val display = viewModel.getChatMessage(item.msgId) ?: viewModel.getStub(item.msgId)?.toSkeletonChatMessage() ?: return
                holder.view.bind(
                    message = display,
                    layout = item.groupLayout,
                    maxBubbleWidthPx = maxBubbleWidth.value.toInt(),
                    highlighted = highlighted,
                )
            }
            is MarkerHolder -> holder.bind(item)
            is ComposeHolder -> {
                if (item !is FeedItem.Message) return
                holder.bind(
                    item = item,
                    highlighted = highlighted,
                    pulseEmoji = rowPulse,
                    contentOnly = payloads.contains(PAYLOAD_CONTENT) && !payloads.contains(PAYLOAD_HIGHLIGHT),
                )
            }
        }
    }

    private fun isTextOnlyRow(message: ChatMessage): Boolean =
        !message.hasAttachment && message.quote == null && message.text.isNotEmpty() && !message.isInfo

    class TextHolder(val view: PolliTextMessageRowView) : RecyclerView.ViewHolder(view)

    class MarkerHolder(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {
        private var boundItem by mutableStateOf<FeedItem?>(null)
        private val newMessagesLabel = composeView.context.getString(R.string.new_messages)

        init {
            composeView.setContent {
                when (val item = boundItem) {
                    is FeedItem.DayMarker -> ChatDayMarkerPill(label = item.label)
                    FeedItem.NewMessages -> ChatNewMessagesPill(label = newMessagesLabel)
                    else -> Unit
                }
            }
        }

        fun bind(item: FeedItem) {
            boundItem = item
        }
    }

    class ComposeHolder(
        val composeView: ComposeView,
        viewModel: ChatViewModel,
        playbackViewModel: PolliAudioPlaybackViewModel?,
        maxBubbleWidth: Dp,
        onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
        onQuoteClick: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(composeView) {
        private var boundItem by mutableStateOf<FeedItem.Message?>(null)
        private var boundHighlighted by mutableStateOf(false)
        private var boundPulseEmoji by mutableStateOf<String?>(null)

        init {
            composeView.setContent {
                val item = boundItem ?: return@setContent
                if (playbackViewModel != null) {
                    CompositionLocalProvider(LocalChatAudioPlayback provides playbackViewModel) {
                        PolliChatFeedRow(
                            viewModel = viewModel,
                            msgId = item.msgId,
                            groupLayout = item.groupLayout,
                            maxBubbleWidth = maxBubbleWidth,
                            highlighted = boundHighlighted,
                            reactionReloadKey = viewModel.reactionEpochFor(item.msgId),
                            pulseEmoji = boundPulseEmoji,
                            onQuoteClick = onQuoteClick,
                            onOpenMessageOverlay = onOpenMessageOverlay,
                        )
                    }
                } else {
                    PolliChatFeedRow(
                        viewModel = viewModel,
                        msgId = item.msgId,
                        groupLayout = item.groupLayout,
                        maxBubbleWidth = maxBubbleWidth,
                        highlighted = boundHighlighted,
                        reactionReloadKey = viewModel.reactionEpochFor(item.msgId),
                        pulseEmoji = boundPulseEmoji,
                        onQuoteClick = onQuoteClick,
                        onOpenMessageOverlay = onOpenMessageOverlay,
                    )
                }
            }
        }

        fun bind(
            item: FeedItem.Message,
            highlighted: Boolean,
            pulseEmoji: String?,
            contentOnly: Boolean,
        ) {
            boundHighlighted = highlighted
            boundPulseEmoji = pulseEmoji
            if (!contentOnly || boundItem?.msgId != item.msgId) {
                boundItem = item
            }
        }
    }
}
