package com.polli.android.chat

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.recyclerview.widget.RecyclerView
import com.polli.android.platform.PlatformDates
import com.polli.android.platform.PolliAudioPlaybackViewModel
import com.polli.core.chat.MessageGroupLayout
import com.polli.domain.model.chat.ChatMessage
import com.polli.domain.model.chat.MSG_ID_DAYMARKER
import java.lang.ref.SoftReference
import java.util.Collections
import java.util.LinkedHashMap

/**
 * DC-style `int[]` adapter — View bind, day markers as normal in-feed rows (not sticky).
 */
class PolliConversationAdapter(
    private val viewModel: ChatViewModel,
    private val maxBubbleWidth: Dp,
    private val playbackViewModel: PolliAudioPlaybackViewModel?,
    private val onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
    private val onQuoteClick: (Int) -> Unit,
) : RecyclerView.Adapter<PolliConversationAdapter.BaseHolder>() {

    companion object {
        private const val MAX_CACHE_SIZE = 40

        private const val TYPE_OUTGOING = 0
        private const val TYPE_INCOMING = 1
        private const val TYPE_INFO = 2
        private const val TYPE_AUDIO_OUTGOING = 3
        private const val TYPE_AUDIO_INCOMING = 4
        private const val TYPE_THUMB_OUTGOING = 5
        private const val TYPE_THUMB_INCOMING = 6
        private const val TYPE_DOCUMENT_OUTGOING = 7
        private const val TYPE_DOCUMENT_INCOMING = 8
        private const val TYPE_STICKER_INCOMING = 9
        private const val TYPE_STICKER_OUTGOING = 10
        private const val TYPE_DAY = 11

        const val PAYLOAD_HIGHLIGHT = "highlight"
        const val PAYLOAD_REACTIONS = "reactions"
    }

    private var dcMsgList: IntArray = intArrayOf()

    private val recordCache: MutableMap<Int, SoftReference<ChatMessage>> =
        Collections.synchronizedMap(
            object : LinkedHashMap<Int, SoftReference<ChatMessage>>(MAX_CACHE_SIZE, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, SoftReference<ChatMessage>>?): Boolean =
                    size > MAX_CACHE_SIZE
            },
        )

    private var highlightMsgId = -1
    private var pulseMsgId = -1
    private var pulseEmoji: String? = null

    init {
        setHasStableIds(true)
    }

    fun changeData(next: IntArray?) {
        val incoming = next ?: intArrayOf()
        if (dcMsgList.contentEquals(incoming)) return
        dcMsgList = incoming
        recordCache.clear()
        notifyDataSetChanged()
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

    fun refreshMessage(msgId: Int) {
        if (msgId <= MSG_ID_DAYMARKER) return
        val pos = positionForMsgId(dcMsgList, msgId)
        if (pos >= 0) notifyItemChanged(pos)
    }

    fun refreshReactionsForPositions(positions: IntArray) {
        for (pos in positions) {
            if (pos in 0 until itemCount) notifyItemChanged(pos, PAYLOAD_REACTIONS)
        }
    }

    fun msgIdToPosition(msgId: Int): Int = positionForMsgId(dcMsgList, msgId)

    override fun getItemCount(): Int = dcMsgList.size

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= dcMsgList.size) return RecyclerView.NO_ID
        val chron = chronIndex(position)
        val id = dcMsgList[chron]
        // Day markers share id 9 — encode chron index for stable unique ids.
        if (id <= MSG_ID_DAYMARKER) return -(chron + 1L)
        return id.toLong()
    }

    fun getMsg(position: Int): ChatMessage? {
        if (position < 0 || position >= dcMsgList.size) return null
        val msgId = dcMsgList[chronIndex(position)]
        if (msgId <= MSG_ID_DAYMARKER) return null
        recordCache[position]?.get()?.let { return it }
        val loaded = viewModel.getChatMessage(msgId) ?: return null
        recordCache[position] = SoftReference(loaded)
        return loaded
    }

    private fun chronIndex(position: Int): Int = chronIndexForPosition(position, dcMsgList)

    private fun dayLabelAt(position: Int): String {
        val chron = chronIndex(position)
        fun labelForId(id: Int): String? {
            if (id <= MSG_ID_DAYMARKER) return null
            val ts = viewModel.getStub(id)?.timestamp ?: viewModel.getChatMessage(id)?.timestamp ?: return null
            if (ts <= 0) return null
            return PlatformDates.relativeDate(viewModel.getApplication(), ts * 1000)
        }
        for (j in chron + 1 until dcMsgList.size) {
            labelForId(dcMsgList[j])?.let { return it }
        }
        for (j in chron - 1 downTo 0) {
            labelForId(dcMsgList[j])?.let { return it }
        }
        return ""
    }

    override fun getItemViewType(position: Int): Int {
        val chron = chronIndex(position)
        val id = dcMsgList.getOrNull(chron) ?: return TYPE_INCOMING
        if (id <= MSG_ID_DAYMARKER) return TYPE_DAY
        val msg = getMsg(position) ?: return TYPE_INCOMING
        if (msg.isInfo) return TYPE_INFO
        return when (msg.viewType) {
            "Audio", "Voice" ->
                if (msg.isOutgoing) TYPE_AUDIO_OUTGOING else TYPE_AUDIO_INCOMING
            "File" ->
                if (msg.isOutgoing) TYPE_DOCUMENT_OUTGOING else TYPE_DOCUMENT_INCOMING
            "Image", "Gif", "Video" ->
                if (msg.isOutgoing) TYPE_THUMB_OUTGOING else TYPE_THUMB_INCOMING
            "Sticker" ->
                if (msg.isOutgoing) TYPE_STICKER_OUTGOING else TYPE_STICKER_INCOMING
            else ->
                if (msg.isOutgoing) TYPE_OUTGOING else TYPE_INCOMING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder {
        val maxPx =
            with(parent.resources.displayMetrics) {
                (maxBubbleWidth.value * density).toInt()
            }
        return when (viewType) {
            TYPE_DAY ->
                DayHolder(
                    ChatDayHeaderView(parent.context).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    },
                )
            TYPE_INFO ->
                InfoHolder(
                    PolliInfoMessageView(parent.context).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    },
                )
            else ->
                MessageHolder(
                    PolliConversationItemView(parent.context).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    },
                    maxPx,
                    playbackViewModel,
                )
        }
    }

    override fun onBindViewHolder(holder: BaseHolder, position: Int) {
        bindInternal(holder, position, emptyList())
    }

    override fun onBindViewHolder(holder: BaseHolder, position: Int, payloads: MutableList<Any>) {
        bindInternal(holder, position, payloads)
    }

    private fun bindInternal(holder: BaseHolder, position: Int, payloads: List<Any>) {
        if (holder is DayHolder) {
            if (payloads.isNotEmpty()) return
            holder.bindLabel(dayLabelAt(position))
            return
        }
        val msg = getMsg(position) ?: return
        val highlighted = msg.id == highlightMsgId
        val rowPulse = if (msg.id == pulseMsgId) pulseEmoji else null
        val highlightOnly = payloads.isNotEmpty() && payloads.all { it == PAYLOAD_HIGHLIGHT }
        val reactionsOnly = payloads.isNotEmpty() && payloads.all { it == PAYLOAD_REACTIONS }
        val reactions = MessageReactions.cachedSummary(msg.id).orEmpty()
        val layout = groupLayoutAtChronIndex(chronIndex(position), dcMsgList) { viewModel.getStub(it) }
        if (reactionsOnly && holder is MessageHolder) {
            holder.bindReactionsOnly(reactions, rowPulse)
            return
        }
        holder.bind(
            message = msg,
            layout = layout,
            reactions = reactions,
            highlighted = highlighted,
            pulseEmoji = rowPulse,
            highlightOnly = highlightOnly,
            onSwipeReply = { viewModel.setReply(msg) },
            onTap = { x, y -> onOpenMessageOverlay(msg, Offset(x, y)) },
            onQuoteClick = onQuoteClick,
        )
    }

    abstract class BaseHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(
            message: ChatMessage,
            layout: MessageGroupLayout,
            reactions: List<BubbleReaction>,
            highlighted: Boolean,
            pulseEmoji: String?,
            highlightOnly: Boolean,
            onSwipeReply: () -> Unit,
            onTap: (Float, Float) -> Unit,
            onQuoteClick: (Int) -> Unit,
        )
    }

    private class DayHolder(
        private val dayView: ChatDayHeaderView,
    ) : BaseHolder(dayView) {
        fun bindLabel(label: String) {
            dayView.setLabel(label)
        }

        override fun bind(
            message: ChatMessage,
            layout: MessageGroupLayout,
            reactions: List<BubbleReaction>,
            highlighted: Boolean,
            pulseEmoji: String?,
            highlightOnly: Boolean,
            onSwipeReply: () -> Unit,
            onTap: (Float, Float) -> Unit,
            onQuoteClick: (Int) -> Unit,
        ) = Unit
    }

    private class InfoHolder(
        private val infoView: PolliInfoMessageView,
    ) : BaseHolder(infoView) {
        override fun bind(
            message: ChatMessage,
            layout: MessageGroupLayout,
            reactions: List<BubbleReaction>,
            highlighted: Boolean,
            pulseEmoji: String?,
            highlightOnly: Boolean,
            onSwipeReply: () -> Unit,
            onTap: (Float, Float) -> Unit,
            onQuoteClick: (Int) -> Unit,
        ) {
            if (highlightOnly) return
            infoView.bind(message)
        }
    }

    private class MessageHolder(
        private val rowView: PolliConversationItemView,
        private val maxBubbleWidthPx: Int,
        private val playbackViewModel: PolliAudioPlaybackViewModel?,
    ) : BaseHolder(rowView) {
        fun bindReactionsOnly(reactions: List<BubbleReaction>, pulseEmoji: String?) {
            rowView.bindReactions(reactions, pulseEmoji)
        }

        override fun bind(
            message: ChatMessage,
            layout: MessageGroupLayout,
            reactions: List<BubbleReaction>,
            highlighted: Boolean,
            pulseEmoji: String?,
            highlightOnly: Boolean,
            onSwipeReply: () -> Unit,
            onTap: (Float, Float) -> Unit,
            onQuoteClick: (Int) -> Unit,
        ) {
            if (highlightOnly) {
                rowView.setHighlighted(highlighted)
                // Pulse can ride on the same chrome payload as highlight.
                if (pulseEmoji != null) {
                    rowView.bindReactions(
                        MessageReactions.cachedSummary(message.id).orEmpty(),
                        pulseEmoji,
                    )
                }
                return
            }
            rowView.bind(
                message = message,
                layout = layout,
                maxBubbleWidthPx = maxBubbleWidthPx,
                reactions = reactions,
                pulseEmoji = pulseEmoji,
                highlighted = highlighted,
                playbackViewModel = playbackViewModel,
                onSwipeReply = onSwipeReply,
                onTap = onTap,
                onQuoteClick = onQuoteClick,
            )
        }
    }
}
