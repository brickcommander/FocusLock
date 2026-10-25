package dev.focuslock.lock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakWindowTest {

    private val window = BreakWindow(startMs = 1_000L, untilMs = 5_000L)

    @Test
    fun `break is active between start and end`() {
        assertTrue(window.isActive(1_000L))
        assertTrue(window.isActive(4_999L))
    }

    @Test
    fun `break is over at its end time`() {
        assertFalse(window.isActive(5_000L))
        assertFalse(window.isActive(9_000L))
    }

    @Test
    fun `moving the clock back before the start ends the break`() {
        assertFalse(window.isActive(999L))
    }
}
