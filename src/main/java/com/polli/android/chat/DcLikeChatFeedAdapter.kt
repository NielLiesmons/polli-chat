package com.polli.android.chat

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.RecyclerView
import com.polli.domain.model.chat.ChatMessage
import java.lang.ref.SoftReference
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Delta Chat-style adapter core:
 * - Backed by an `int[]` message-id list
 * - Stable ids
 * - Position-keyed LRU cache (size 40) to keep getItemViewType() and bind() cheap
 * - Typed ViewHolders (start with text/info/generic; expand to media/voice/etc.)
 */
class DcLikeChatFeedAdapter(
    private val context: Context,
    private val viewModel: ChatViewModel,
    private val maxBubbleWidthPxProvider: () -> Int,
) : RecyclerView.Adapter<DcLikeChatFeedAdapter.BaseHolder>() {

    companion object {
        private const val MAX_CACHE_SIZE = 40

        private const val TYPE_TEXT = 0
        private const val TYPE_INFO = 1
        private const val TYPE_GENERIC = 2
    }

    private var msgIds: IntArray = intArrayOf()

    // Position-keyed, like DC.
    private val recordCache: MutableMap<Int, SoftReference<ChatMessage>> =
        Collections.synchronizedMap(
            object : LinkedHashMap<Int, SoftReference<ChatMessage>>(MAX_CACHE_SIZE, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, SoftReference<ChatMessage>>?): Boolean {
                    return size > MAX_CACHE_SIZE
                }
            },
        )

    init {
        setHasStableIds(true)
    }

    fun changeData(next: IntArray?) {
        msgIds = next ?: intArrayOf()
        reloadData()
    }

    private fun reloadData() {
        recordCache.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = msgIds.size

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= msgIds.size) return 0L
        // reverseLayout=true list: adapter position 0 is newest, so stable id uses reversed index
        return msgIds[msgIds.size - 1 - position].toLong()
    }

    private fun getMsg(position: Int): ChatMessage? {
        if (position < 0 || position >= msgIds.size) return null

        recordCache[position]?.get()?.let { return it }

        val msgId = getItemId(position).toInt()
        val loaded = viewModel.getChatMessage(msgId) ?: return null
        recordCache[position] = SoftReference(loaded)
        return loaded
    }

    override fun getItemViewType(position: Int): Int {
        val msg = getMsg(position) ?: return TYPE_GENERIC
        return when {
            msg.isInfo -> TYPE_INFO
            msg.viewType == "Text" && !msg.hasAttachment && msg.quote == null -> TYPE_TEXT
            else -> TYPE_GENERIC
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder {
        return when (viewType) {
            TYPE_TEXT -> {
                val view = PolliTextMessageRowView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                }
                TextHolder(view)
            }
            TYPE_INFO -> {
                val view = PolliTextMessageRowView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                }
                InfoHolder(view)
            }
            else -> {
                val view = PolliTextMessageRowView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                }
                GenericHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: BaseHolder, position: Int) {
        val msgId = getItemId(position).toInt()
        val msg = getMsg(position) ?: return
        val layout = viewModel.groupLayoutForMsgId(msgId)
        holder.bind(
            message = msg,
            layout = layout,
            maxBubbleWidthPx = maxBubbleWidthPxProvider(),
        )
    }

    sealed class BaseHolder(
        protected val rowView: PolliTextMessageRowView,
    ) : RecyclerView.ViewHolder(rowView) {
        abstract fun bind(message: ChatMessage, layout: com.polli.core.chat.MessageGroupLayout, maxBubbleWidthPx: Int)
    }

    private class TextHolder(rowView: PolliTextMessageRowView) : BaseHolder(rowView) {
        override fun bind(message: ChatMessage, layout: com.polli.core.chat.MessageGroupLayout, maxBubbleWidthPx: Int) {
            rowView.bind(message, layout, maxBubbleWidthPx, highlighted = false)
        }
    }

    private class InfoHolder(rowView: PolliTextMessageRowView) : BaseHolder(rowView) {
        override fun bind(message: ChatMessage, layout: com.polli.core.chat.MessageGroupLayout, maxBubbleWidthPx: Int) {
            rowView.bind(message, layout, maxBubbleWidthPx, highlighted = false)
        }
    }

    private class GenericHolder(rowView: PolliTextMessageRowView) : BaseHolder(rowView) {
        override fun bind(message: ChatMessage, layout: com.polli.core.chat.MessageGroupLayout, maxBubbleWidthPx: Int) {
            rowView.bind(message, layout, maxBubbleWidthPx, highlighted = false)
        }
    }
}

