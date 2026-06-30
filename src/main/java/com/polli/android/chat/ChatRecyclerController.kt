package com.polli.android.chat

import androidx.compose.runtime.mutableStateOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Bridges RecyclerView scroll state to Compose chrome (FAB, unread, scroll-to-quote). */
class ChatRecyclerController {
    internal var recyclerView: RecyclerView? = null

    var showScrollToBottom = mutableStateOf(false)
        private set

    fun isAtChatBottom(): Boolean {
        val list = recyclerView ?: return true
        val lm = list.layoutManager as? LinearLayoutManager ?: return true
        val first = lm.findFirstVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION) return true
        if (first != 0) return false
        val child = list.getChildAt(0) ?: return true
        return child.bottom <= list.height - list.paddingBottom
    }

    fun updateScrollFabVisibility() {
        showScrollToBottom.value = !isAtChatBottom()
    }

    fun scrollToBottom(animated: Boolean) {
        val list = recyclerView ?: return
        val lm = list.layoutManager as? LinearLayoutManager ?: return
        val first = lm.findFirstVisibleItemPosition()
        if (animated && first in 0 until SCROLL_ANIMATION_THRESHOLD) {
            list.smoothScrollToPosition(0)
        } else {
            list.scrollToPosition(0)
        }
    }

    fun scrollMaybeSmoothToDisplayIndex(displayIndex: Int) {
        val list = recyclerView ?: return
        val lm = list.layoutManager as? LinearLayoutManager ?: return
        val index = displayIndex.coerceAtLeast(0)
        val first = lm.findFirstVisibleItemPosition()
        val last = lm.findLastVisibleItemPosition()
        val distance = minOf(
            if (first == RecyclerView.NO_POSITION) Int.MAX_VALUE else kotlin.math.abs(index - first),
            if (last == RecyclerView.NO_POSITION) Int.MAX_VALUE else kotlin.math.abs(index - last),
        )
        if (distance < SMOOTH_SCROLL_DISTANCE_THRESHOLD) {
            list.smoothScrollToPosition(index)
        } else {
            list.scrollToPosition(index)
        }
    }

    private companion object {
        const val SCROLL_ANIMATION_THRESHOLD = 50
        const val SMOOTH_SCROLL_DISTANCE_THRESHOLD = 15
    }
}
