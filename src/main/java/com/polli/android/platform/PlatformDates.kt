package com.polli.android.platform

import android.content.Context
import com.polli.android.util.DateUtils

object PlatformDates {
    fun relativeDate(context: Context, timestampMs: Long): String =
        DateUtils.getRelativeDate(context, timestampMs)

    fun extendedTimeSpan(context: Context, timestampMs: Long): String =
        DateUtils.getExtendedTimeSpanString(context, timestampMs)
}
