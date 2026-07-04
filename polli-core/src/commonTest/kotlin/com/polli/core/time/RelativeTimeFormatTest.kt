package com.polli.core.time

import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeFormatTest {
    @Test
    fun formatsRecentTimes() {
        val now = 1_000_000L
        assertEquals("now", RelativeTimeFormat.format(now - 30, now))
        assertEquals("5m", RelativeTimeFormat.format(now - 300, now))
        assertEquals("2h", RelativeTimeFormat.format(now - 7200, now))
        assertEquals("3d", RelativeTimeFormat.format(now - 259_200, now))
        assertEquals("1w+", RelativeTimeFormat.format(now - 604_800, now))
    }

    @Test
    fun zeroTimestampShowsDash() {
        assertEquals("—", RelativeTimeFormat.format(0, 100))
    }
}
