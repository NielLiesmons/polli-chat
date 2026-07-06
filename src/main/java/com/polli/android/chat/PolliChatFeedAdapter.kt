package com.polli.android.chat

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.FeedItem
import com.polli.domain.model.chat.displayIndexForMessage
import com.polli.domain.model.chat.stableRowId
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliTheme
import com.polli.ui.components.chat.ChatDayMarkerPill
import com.polli.ui.components.chat.ChatNewMessagesPill
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.audioplay.AudioPlaybackViewModel

private data class FeedRowKey(
    val stableId: Long,
    val label: String?,
    val olderMsgId: Int,
    val selfMsgId: Int,
    val newerMsgId: Int,
)

/**
 * DC [org.thoughtcrime.securesms.ConversationAdapter]: feed rows + lazy [com.b44t.messenger.DcMsg] per bind.
 * Rows are Polli Compose bubbles inside recycled [ComposeView]s — composition is installed once per holder.
 */
class PolliChatFeedAdapter(
    private val viewModel: ChatViewModel,
    private val prefs: AppPrefs,
    private val uiScaleRevision: Int,
    private val playbackViewModel: AudioPlaybackViewModel?,
    private val onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
    private val onQuoteClick: (Int) -> Unit,
) : RecyclerView.Adapter<PolliChatFeedAdapter.Holder>() {

    private var displayItems: List<FeedItem> = emptyList()

    init {
        setHasStableIds(true)
    }

    /** @return true when the feed changed */
    fun changeData(items: List<FeedItem>): Boolean {
        if (items == displayItems) return false
        val oldItems = displayItems
        val diff =
            DiffUtil.calculateDiff(
                object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int = oldItems.size

                    override fun getNewListSize(): Int = items.size

                    override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
                        return rowKey(oldItems, oldPosition).stableId == rowKey(items, newPosition).stableId
                    }

                    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
                        return rowKey(oldItems, oldPosition) == rowKey(items, newPosition)
                    }
                },
            )
        displayItems = items
        diff.dispatchUpdatesTo(this)
        return true
    }

    override fun getItemCount(): Int = displayItems.size

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= displayItems.size) return RecyclerView.NO_ID
        return chronItem(position).stableRowId()
    }

    fun displayIndexForMsgId(msgId: Int): Int = displayItems.displayIndexForMessage(msgId)

    private fun chronIndex(displayPosition: Int): Int = displayItems.size - 1 - displayPosition

    private fun chronItem(displayPosition: Int): FeedItem = displayItems[chronIndex(displayPosition)]

    private fun rowKey(items: List<FeedItem>, displayPosition: Int): FeedRowKey {
        val item = items[chronIndex(displayPosition)]
        val (older, newer) = messageNeighborIds(items, displayPosition)
        return when (item) {
            is FeedItem.DayMarker ->
                FeedRowKey(
                    stableId = item.stableRowId(),
                    label = item.label,
                    olderMsgId = 0,
                    selfMsgId = 0,
                    newerMsgId = 0,
                )
            is FeedItem.Message ->
                FeedRowKey(
                    stableId = item.stableRowId(),
                    label = null,
                    olderMsgId = older ?: 0,
                    selfMsgId = item.msgId,
                    newerMsgId = newer ?: 0,
                )
            else ->
                FeedRowKey(
                    stableId = item.stableRowId(),
                    label = null,
                    olderMsgId = 0,
                    selfMsgId = 0,
                    newerMsgId = 0,
                )
        }
    }

    private fun messageNeighborIds(items: List<FeedItem>, displayPosition: Int): Pair<Int?, Int?> {
        val chron = items.size - 1 - displayPosition
        var older: Int? = null
        var newer: Int? = null
        for (i in chron - 1 downTo 0) {
            val row = items[i]
            if (row is FeedItem.Message) {
                older = row.msgId
                break
            }
        }
        for (i in chron + 1 until items.size) {
            val row = items[i]
            if (row is FeedItem.Message) {
                newer = row.msgId
                break
            }
        }
        return older to newer
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val composeView =
            ComposeView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            }
        return Holder(
            composeView = composeView,
            viewModel = viewModel,
            prefs = prefs,
            uiScaleRevision = uiScaleRevision,
            playbackViewModel = playbackViewModel,
            onOpenMessageOverlay = onOpenMessageOverlay,
            onQuoteClick = onQuoteClick,
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = chronItem(position)
        val (olderMsgId, newerMsgId) = messageNeighborIds(displayItems, position)
        holder.bind(item, olderMsgId, newerMsgId)
    }

    class Holder(
        val composeView: ComposeView,
        viewModel: ChatViewModel,
        prefs: AppPrefs,
        uiScaleRevision: Int,
        playbackViewModel: AudioPlaybackViewModel?,
        onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
        onQuoteClick: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(composeView) {
        private var boundItem by mutableStateOf<FeedItem?>(null)
        private var boundOlderMsgId by mutableStateOf<Int?>(null)
        private var boundNewerMsgId by mutableStateOf<Int?>(null)
        private val newMessagesLabel = composeView.context.getString(R.string.new_messages)

        init {
            composeView.setContent {
                PolliTheme(prefs = prefs, uiScaleRevision = uiScaleRevision) {
                    when (val item = boundItem) {
                        is FeedItem.DayMarker -> ChatDayMarkerPill(label = item.label)
                        FeedItem.NewMessages -> ChatNewMessagesPill(label = newMessagesLabel)
                        is FeedItem.Message -> {
                            val msgId = item.msgId
                            if (playbackViewModel != null) {
                                CompositionLocalProvider(LocalChatAudioPlayback provides playbackViewModel) {
                                    PolliChatFeedRow(
                                        viewModel = viewModel,
                                        msgId = msgId,
                                        olderMsgId = boundOlderMsgId,
                                        newerMsgId = boundNewerMsgId,
                                        onQuoteClick = onQuoteClick,
                                        onOpenMessageOverlay = onOpenMessageOverlay,
                                    )
                                }
                            } else {
                                PolliChatFeedRow(
                                    viewModel = viewModel,
                                    msgId = msgId,
                                    olderMsgId = boundOlderMsgId,
                                    newerMsgId = boundNewerMsgId,
                                    onQuoteClick = onQuoteClick,
                                    onOpenMessageOverlay = onOpenMessageOverlay,
                                )
                            }
                        }
                        null -> Unit
                    }
                }
            }
        }

        fun bind(item: FeedItem, olderMsgId: Int?, newerMsgId: Int?) {
            boundItem = item
            boundOlderMsgId = olderMsgId
            boundNewerMsgId = newerMsgId
        }
    }
}
