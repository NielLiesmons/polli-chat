package com.polli.android.chat

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import com.b44t.messenger.DcMsg
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.LabTheme
import org.thoughtcrime.securesms.components.audioplay.AudioPlaybackViewModel
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset

/**
 * DC [org.thoughtcrime.securesms.ConversationAdapter]: id array + lazy [com.b44t.messenger.DcMsg] per bind.
 * Rows are Polli Compose bubbles inside recycled [ComposeView]s.
 */
class PolliChatFeedAdapter(
    private val viewModel: ChatViewModel,
    private val prefs: AppPrefs,
    private val uiScaleRevision: Int,
    private val playbackViewModel: AudioPlaybackViewModel?,
    private val onOpenMessageOverlay: (ChatMessage, Offset) -> Unit,
    private val onQuoteClick: (Int) -> Unit,
) : RecyclerView.Adapter<PolliChatFeedAdapter.Holder>() {

    private var displayIds: IntArray = intArrayOf()

    init {
        setHasStableIds(true)
    }

    fun changeData(ids: IntArray) {
        displayIds = ids.filter { it > DcMsg.DC_MSG_ID_DAYMARKER }.toIntArray()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = displayIds.size

    override fun getItemId(position: Int): Long {
        if (position < 0 || position >= displayIds.size) return 0L
        return displayIds[displayIds.size - 1 - position].toLong()
    }

    fun msgIdAt(position: Int): Int = displayIds[displayIds.size - 1 - position]

    fun displayIndexForMsgId(msgId: Int): Int {
        val chron = displayIds.indexOf(msgId)
        return if (chron < 0) -1 else displayIds.size - 1 - chron
    }

    fun neighborIds(position: Int): Pair<Int?, Int?> {
        val chron = displayIds.size - 1 - position
        val older = displayIds.getOrNull(chron - 1)
        val newer = displayIds.getOrNull(chron + 1)
        return older to newer
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val composeView = ComposeView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        return Holder(composeView)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val msgId = msgIdAt(position)
        val (olderMsgId, newerMsgId) = neighborIds(position)
        holder.composeView.setContent {
            LabTheme(prefs = prefs, uiScaleRevision = uiScaleRevision) {
                if (playbackViewModel != null) {
                    CompositionLocalProvider(LocalChatAudioPlayback provides playbackViewModel) {
                        PolliChatFeedRow(
                            viewModel = viewModel,
                            msgId = msgId,
                            olderMsgId = olderMsgId,
                            newerMsgId = newerMsgId,
                            onQuoteClick = onQuoteClick,
                            onOpenMessageOverlay = onOpenMessageOverlay,
                        )
                    }
                } else {
                    PolliChatFeedRow(
                        viewModel = viewModel,
                        msgId = msgId,
                        olderMsgId = olderMsgId,
                        newerMsgId = newerMsgId,
                        onQuoteClick = onQuoteClick,
                        onOpenMessageOverlay = onOpenMessageOverlay,
                    )
                }
            }
        }
    }

    class Holder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)
}
