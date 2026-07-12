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
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.FeedItem
import com.polli.domain.model.chat.displayIndexForMessage
import com.polli.domain.model.chat.stableRowId
import com.polli.android.platform.PolliAudioPlaybackViewModel
import com.polli.ui.components.chat.ChatDayMarkerPill
import com.polli.ui.components.chat.ChatNewMessagesPill
import com.polli.android.R

/**
 * DC [org.thoughtcrime.securesms.ConversationAdapter]: Compose bubble rows only (Polli UI).
 */
class PolliChatFeedAdapter(
    private val viewModel: ChatViewModel,
    private val playbackViewModel: PolliAudioPlaybackViewModel?,
    private val maxBubbleWidth: Dp,
    private val onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
    private val onQuoteClick: (Int) -> Unit,
) : RecyclerView.Adapter<PolliChatFeedAdapter.Holder>() {

    companion object {
        private const val VIEW_TYPE_MARKER = 0
        private const val VIEW_TYPE_MESSAGE = 1
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

    override fun getItemViewType(position: Int): Int =
        when (chronItem(position)) {
            is FeedItem.DayMarker, FeedItem.NewMessages -> VIEW_TYPE_MARKER
            else -> VIEW_TYPE_MESSAGE
        }

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= displayItems.size) return RecyclerView.NO_ID
        return chronItem(position).stableRowId()
    }

    fun displayIndexForMsgId(msgId: Int): Int = displayItems.displayIndexForMessage(msgId)

    private fun chronIndex(displayPosition: Int): Int = displayItems.size - 1 - displayPosition

    private fun chronItem(displayPosition: Int): FeedItem = displayItems[chronIndex(displayPosition)]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val composeView =
            ComposeView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                // DC recycles the same View and calls bind() — keep composition alive across detach.
                setViewCompositionStrategy(ViewCompositionStrategy.Default)
                (parent as? View)?.findViewTreeCompositionContext()?.let { setParentCompositionContext(it) }
            }
        return Holder(
            composeView = composeView,
            viewModel = viewModel,
            playbackViewModel = playbackViewModel,
            maxBubbleWidth = maxBubbleWidth,
            onOpenMessageOverlay = onOpenMessageOverlay,
            onQuoteClick = onQuoteClick,
            markerMode = viewType == VIEW_TYPE_MARKER,
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        bindInternal(holder, position, emptyList())
    }

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        bindInternal(holder, position, payloads)
    }

    private fun bindInternal(holder: Holder, position: Int, payloads: List<Any>) {
        val item = chronItem(position)
        val highlighted = item is FeedItem.Message && item.msgId == highlightMsgId
        val rowPulse = if (item is FeedItem.Message && item.msgId == pulseMsgId) pulseEmoji else null
        holder.bind(
            item = item,
            highlighted = highlighted,
            pulseEmoji = rowPulse,
            contentOnly = payloads.contains(PAYLOAD_CONTENT) && !payloads.contains(PAYLOAD_HIGHLIGHT),
        )
    }

    class Holder(
        val composeView: ComposeView,
        private val viewModel: ChatViewModel,
        playbackViewModel: PolliAudioPlaybackViewModel?,
        maxBubbleWidth: Dp,
        onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
        onQuoteClick: (Int) -> Unit,
        private val markerMode: Boolean,
    ) : RecyclerView.ViewHolder(composeView) {
        private var boundItem by mutableStateOf<FeedItem?>(null)
        private var boundMessage by mutableStateOf<ChatMessage?>(null)
        private var boundHighlighted by mutableStateOf(false)
        private var boundPulseEmoji by mutableStateOf<String?>(null)
        private val newMessagesLabel = composeView.context.getString(R.string.new_messages)

        init {
            composeView.setContent {
                when (val item = boundItem) {
                    is FeedItem.DayMarker -> ChatDayMarkerPill(label = item.label)
                    FeedItem.NewMessages -> ChatNewMessagesPill(label = newMessagesLabel)
                    is FeedItem.Message -> {
                        val message = boundMessage ?: return@setContent
                        if (playbackViewModel != null) {
                            CompositionLocalProvider(LocalChatAudioPlayback provides playbackViewModel) {
                                PolliChatFeedRow(
                                    message = message,
                                    groupLayout = item.groupLayout,
                                    maxBubbleWidth = maxBubbleWidth,
                                    highlighted = boundHighlighted,
                                    reactionReloadKey = viewModel.reactionEpochFor(item.msgId),
                                    pulseEmoji = boundPulseEmoji,
                                onSwipeReply = { viewModel.setReply(message) },
                                onOpenMessageOverlay = onOpenMessageOverlay,
                                onQuoteClick = onQuoteClick,
                                )
                            }
                        } else {
                            PolliChatFeedRow(
                                message = message,
                                groupLayout = item.groupLayout,
                                maxBubbleWidth = maxBubbleWidth,
                                highlighted = boundHighlighted,
                                reactionReloadKey = viewModel.reactionEpochFor(item.msgId),
                                pulseEmoji = boundPulseEmoji,
                                onSwipeReply = { viewModel.setReply(message) },
                                onOpenMessageOverlay = onOpenMessageOverlay,
                                onQuoteClick = onQuoteClick,
                            )
                        }
                    }
                    null -> Unit
                }
            }
        }

        fun bind(
            item: FeedItem,
            highlighted: Boolean,
            pulseEmoji: String?,
            contentOnly: Boolean,
        ) {
            if (markerMode && item !is FeedItem.DayMarker && item != FeedItem.NewMessages) return
            if (!markerMode && item !is FeedItem.Message) return
            boundHighlighted = highlighted
            boundPulseEmoji = pulseEmoji
            if (!contentOnly || boundItem != item) {
                boundItem = item
            }
            if (item is FeedItem.Message) {
                val msg = viewModel.getChatMessage(item.msgId)
                val stub = viewModel.getStub(item.msgId)
                boundMessage = msg ?: stub?.toSkeletonChatMessage()
            }
        }
    }
}
