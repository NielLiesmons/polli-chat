package com.polli.android.chat

import android.content.Context
import android.os.Parcelable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** DC [org.thoughtcrime.securesms.SetStartingPositionLinearLayoutManager] — stack from end, optional open position. */
class PolliChatLayoutManager(context: Context) : LinearLayoutManager(context, VERTICAL, true) {

    private var pendingStartingPos = -1

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (pendingStartingPos != -1 && state.itemCount > 0) {
            val position = pendingStartingPos + 1
            if (position < state.itemCount) {
                scrollToPositionWithOffset(pendingStartingPos + 1, height - (height / 40))
            } else {
                scrollToPosition(pendingStartingPos)
            }
            pendingStartingPos = -1
        }
        super.onLayoutChildren(recycler, state)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        pendingStartingPos = -1
        super.onRestoreInstanceState(state)
    }

    fun setStartingPosition(position: Int) {
        pendingStartingPos = position
    }
}
