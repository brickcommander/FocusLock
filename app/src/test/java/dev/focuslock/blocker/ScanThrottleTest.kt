package dev.focuslock.blocker

import dev.focuslock.pin.FakeClock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanThrottleTest {

    private val clock = FakeClock()
    private val throttle = ScanThrottle(clock, intervalMs = 300)

    @Test
    fun `first content change scans`() {
        assertTrue(throttle.shouldScan(isWindowStateChange = false))
    }

    @Test
    fun `window change always scans`() {
        throttle.shouldScan(isWindowStateChange = true)

        assertTrue(throttle.shouldScan(isWindowStateChange = true))
    }

    @Test
    fun `content change within interval is skipped`() {
        throttle.shouldScan(isWindowStateChange = true)
        clock.advance(299)

        assertFalse(throttle.shouldScan(isWindowStateChange = false))
    }

    @Test
    fun `content change after interval scans`() {
        throttle.shouldScan(isWindowStateChange = false)
        clock.advance(300)

        assertTrue(throttle.shouldScan(isWindowStateChange = false))
    }
}
