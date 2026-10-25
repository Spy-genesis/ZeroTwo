package com.zerotwo.focus.screentime

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDurationTest {

    @Test
    fun formats() {
        assertEquals("<1m", formatDuration(59_000))
        assertEquals("1m", formatDuration(60_000))
        assertEquals("59m", formatDuration(59 * 60_000L))
        assertEquals("1h 00m", formatDuration(60 * 60_000L))
        assertEquals("2h 05m", formatDuration(125 * 60_000L))
    }
}
