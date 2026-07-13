package com.polli.android.chat

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * DC [org.thoughtcrime.securesms.util.StickyHeaderDecoration] — day pills overlay the list,
 * not separate adapter rows.
 */
class StickyHeaderDecoration(
    private val adapter: StickyHeaderAdapter,
    private val sticky: Boolean = true,
) : RecyclerView.ItemDecoration() {

    interface StickyHeaderAdapter {
        fun getHeaderId(position: Int): Long
        fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder
        fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int)
    }

    abstract class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val headerCache = HashMap<Long, HeaderViewHolder>()

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        var headerHeight = 0
        if (position != RecyclerView.NO_POSITION && hasHeader(parent, position)) {
            headerHeight = headerHeight(getHeader(parent, position).itemView)
        }
        outRect.set(0, headerHeight, 0, 0)
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val count = parent.childCount
        for (layoutPos in 0 until count) {
            val child = parent.getChildAt(translatedChildPosition(parent, layoutPos))
            val adapterPos = parent.getChildAdapterPosition(child)
            if (adapterPos != RecyclerView.NO_POSITION &&
                ((layoutPos == 0 && sticky) || hasHeader(parent, adapterPos))
            ) {
                val header = getHeader(parent, adapterPos).itemView
                canvas.save()
                canvas.translate(child.left.toFloat(), headerTop(parent, child, header, adapterPos, layoutPos).toFloat())
                header.draw(canvas)
                canvas.restore()
            }
        }
    }

    private fun hasHeader(parent: RecyclerView, adapterPos: Int): Boolean {
        val reverse = isReverseLayout(parent)
        val itemCount = parent.adapter?.itemCount ?: 0
        if ((reverse && adapterPos == itemCount - 1 && adapter.getHeaderId(adapterPos) != NO_HEADER_ID) ||
            (!reverse && adapterPos == 0)
        ) {
            return true
        }
        val previous = adapterPos + if (reverse) 1 else -1
        val headerId = adapter.getHeaderId(adapterPos)
        val previousHeaderId = adapter.getHeaderId(previous)
        return headerId != NO_HEADER_ID &&
            previousHeaderId != NO_HEADER_ID &&
            headerId != previousHeaderId
    }

    private fun getHeader(parent: RecyclerView, position: Int): HeaderViewHolder {
        val key = adapter.getHeaderId(position)
        headerCache[key]?.let { return it }
        val holder = adapter.onCreateHeaderViewHolder(parent)
        adapter.onBindHeaderViewHolder(holder, position)
        val header = holder.itemView
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)
        val childWidth =
            ViewGroup.getChildMeasureSpec(
                widthSpec,
                parent.paddingLeft + parent.paddingRight,
                header.layoutParams.width,
            )
        val childHeight =
            ViewGroup.getChildMeasureSpec(
                heightSpec,
                parent.paddingTop + parent.paddingBottom,
                header.layoutParams.height,
            )
        header.measure(childWidth, childHeight)
        header.layout(0, 0, header.measuredWidth, header.measuredHeight)
        headerCache[key] = holder
        return holder
    }

    private fun headerTop(
        parent: RecyclerView,
        child: View,
        header: View,
        adapterPos: Int,
        layoutPos: Int,
    ): Int {
        val headerHeight = headerHeight(header)
        var top = childTop(parent, child) - headerHeight
        if (sticky && layoutPos == 0) {
            val count = parent.childCount
            val currentId = adapter.getHeaderId(adapterPos)
            for (i in 1 until count) {
                val posHere = parent.getChildAdapterPosition(parent.getChildAt(translatedChildPosition(parent, i)))
                if (posHere != RecyclerView.NO_POSITION) {
                    val nextId = adapter.getHeaderId(posHere)
                    if (nextId != currentId) {
                        val next = parent.getChildAt(translatedChildPosition(parent, i))
                        val offset = childTop(parent, next) - (headerHeight + headerHeight(getHeader(parent, posHere).itemView))
                        if (offset < 0) top += offset
                        break
                    }
                }
            }
        }
        return top
    }

    private fun headerHeight(header: View): Int {
        if (header.height > 0) return header.height
        return header.measuredHeight
    }

    private fun childTop(parent: RecyclerView, child: View): Int = child.top - parent.paddingTop

    private fun isReverseLayout(parent: RecyclerView): Boolean {
        val lm = parent.layoutManager as? LinearLayoutManager ?: return false
        return lm.reverseLayout
    }

    private fun translatedChildPosition(parent: RecyclerView, layoutPos: Int): Int =
        if (isReverseLayout(parent)) parent.childCount - 1 - layoutPos else layoutPos

    fun clearHeaderCache() {
        headerCache.clear()
    }

    private companion object {
        const val NO_HEADER_ID = -1L
    }
}
