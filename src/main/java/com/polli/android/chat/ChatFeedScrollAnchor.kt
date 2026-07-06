package com.polli.android.chat

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

internal data class CapturedScrollAnchor(
    val index: Int,
    val pixelOffset: Int,
    val oldCount: Int,
)

internal object ChatFeedScrollAnchor {
    fun capture(recycler: RecyclerView, layoutManager: LinearLayoutManager, oldCount: Int): CapturedScrollAnchor? {
        if (oldCount <= 0) return null
        val oldIndex = layoutManager.findFirstVisibleItemPosition()
        if (oldIndex == RecyclerView.NO_POSITION || oldIndex <= 0) return null
        val firstView = layoutManager.findViewByPosition(oldIndex) ?: return null
        val pixelOffset = recycler.bottom - firstView.bottom - recycler.paddingBottom
        return CapturedScrollAnchor(oldIndex, pixelOffset, oldCount)
    }

    fun restore(layoutManager: LinearLayoutManager, anchor: CapturedScrollAnchor, newCount: Int) {
        if (newCount <= 0 || anchor.index <= 0) return
        var newIndex = anchor.index + newCount - anchor.oldCount
        var pixelOffset = anchor.pixelOffset
        when {
            newIndex < 0 -> {
                newIndex = 0
                pixelOffset = 0
            }
            newIndex >= newCount -> {
                newIndex = newCount - 1
                pixelOffset = 0
            }
        }
        layoutManager.scrollToPositionWithOffset(newIndex, pixelOffset)
    }
}
