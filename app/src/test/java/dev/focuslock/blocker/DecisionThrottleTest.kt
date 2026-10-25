package dev.focuslock.blocker

import dev.focuslock.pin.FakeClock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionThrottleTest {

    private val clock = FakeClock()
    private val throttle = DecisionThrottle(clock)
    private val instagram = Decision.BlockApp("com.instagram.android")

    @Test
    fun `first block fires`() {
        assertTrue(throttle.shouldFire(instagram))
    }

    @Test
    fun `same block within 1s skipped`() {
        throttle.shouldFire(instagram)
        clock.advance(999)

        assertFalse(throttle.shouldFire(instagram))
    }

    @Test
    fun `same block after 1s fires`() {
        throttle.shouldFire(instagram)
        clock.advance(1_000)

        assertTrue(throttle.shouldFire(instagram))
    }

    @Test
    fun `different block fires immediately`() {
        throttle.shouldFire(instagram)

        val fired = throttle.shouldFire(Decision.BlockSite("youtube.com"))

        assertTrue(fired)
    }

    @Test
    fun `allow never fires`() {
        assertFalse(throttle.shouldFire(Decision.Allow))
        clock.advance(5_000)
        assertFalse(throttle.shouldFire(Decision.Allow))
    }
}
