package com.polli.android.chat

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.recyclerview.widget.RecyclerView
import com.polli.domain.model.chat.ChatMessage
import com.polli.android.platform.PolliAudioPlaybackViewModel
import com.polli.android.platform.PlatformDates
import java.lang.ref.SoftReference
import java.util.Collections
import java.util.LinkedHashMap

/**
 * DC [org.thoughtcrime.securesms.ConversationAdapter] — `int[]` + View bind, no Compose on scroll path.
 */
class PolliConversationAdapter(
    private val viewModel: ChatViewModel,
    private val maxBubbleWidth: Dp,
    private val playbackViewModel: PolliAudioPlaybackViewModel?,
    private val onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
    private val onQuoteClick: (Int) -> Unit,
) : RecyclerView.Adapter<PolliConversationAdapter.BaseHolder>(),
    StickyHeaderDecoration.StickyHeaderAdapter {

    companion object {
        private const val MAX_CACHE_SIZE = 40
        const val NO_HEADER_ID = -1L

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

        const val PAYLOAD_HIGHLIGHT = "highlight"
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
        dcMsgList = next ?: intArrayOf()
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
        val pos = positionForMsgId(dcMsgList, msgId)
        if (pos >= 0) notifyItemChanged(pos)
    }

    fun msgIdToPosition(msgId: Int): Int = positionForMsgId(dcMsgList, msgId)

    override fun getItemCount(): Int = dcMsgList.size

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= dcMsgList.size) return RecyclerView.NO_ID
        return dcMsgList[dcMsgList.size - 1 - position].toLong()
    }

    fun getMsg(position: Int): ChatMessage? {
        if (position < 0 || position >= dcMsgList.size) return null
        recordCache[position]?.get()?.let { return it }
        val msgId = getItemId(position).toInt()
        val loaded = viewModel.getChatMessage(msgId) ?: return null
        recordCache[position] = SoftReference(loaded)
        return loaded
    }

    private fun chronIndex(position: Int): Int = chronIndexForPosition(position, dcMsgList)

    override fun getItemViewType(position: Int): Int {
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
        val msg = getMsg(position) ?: return
        val highlighted = msg.id == highlightMsgId
        val rowPulse = if (msg.id == pulseMsgId) pulseEmoji else null
        val highlightOnly = payloads.isNotEmpty() && payloads.all { it == PAYLOAD_HIGHLIGHT }
        val layout = groupLayoutAtChronIndex(chronIndex(position), dcMsgList) { viewModel.getStub(it) }
        val reactions = MessageReactions.loadReactionSummary(holder.itemView.context, msg.id)
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

    override fun getHeaderId(position: Int): Long {
        if (position !in 0 until itemCount) return NO_HEADER_ID
        val ts = getMsg(position)?.timestamp ?: return NO_HEADER_ID
        return dayHeaderId(ts)
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): StickyHeaderDecoration.HeaderViewHolder {
        val view = ChatDayHeaderView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        return DayHeaderHolder(view)
    }

    override fun onBindHeaderViewHolder(holder: StickyHeaderDecoration.HeaderViewHolder, position: Int) {
        val header = holder as DayHeaderHolder
        val ts = getMsg(position)?.timestamp ?: 0L
        header.view.setLabel(PlatformDates.relativeDate(header.view.context, ts * 1000))
    }

    abstract class BaseHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(
            message: ChatMessage,
            layout: com.polli.core.chat.MessageGroupLayout,
            reactions: List<BubbleReaction>,
            highlighted: Boolean,
            pulseEmoji: String?,
            highlightOnly: Boolean,
            onSwipeReply: () -> Unit,
            onTap: (Float, Float) -> Unit,
            onQuoteClick: (Int) -> Unit,
        )
    }

    private class DayHeaderHolder(val view: ChatDayHeaderView) : StickyHeaderDecoration.HeaderViewHolder(view)

    private class InfoHolder(
        private val infoView: PolliInfoMessageView,
    ) : BaseHolder(infoView) {
        override fun bind(
            message: ChatMessage,
            layout: com.polli.core.chat.MessageGroupLayout,
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
        override fun bind(
            message: ChatMessage,
            layout: com.polli.core.chat.MessageGroupLayout,
            reactions: List<BubbleReaction>,
            highlighted: Boolean,
            pulseEmoji: String?,
            highlightOnly: Boolean,
            onSwipeReply: () -> Unit,
            onTap: (Float, Float) -> Unit,
            onQuoteClick: (Int) -> Unit,
        ) {
            if (highlightOnly) {
                rowView.alpha = if (highlighted) 1f else 0.98f
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
