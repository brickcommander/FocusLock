package dev.focuslock.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockDeciderFocusAndLimitsTest {

    private val decider = BlockDecider(SettingsGuard())

    private fun event(pkg: String, url: String? = null, texts: List<String> = emptyList()) =
        ScreenEvent(packageName = pkg, urlBarText = url, screenTexts = texts)

    @Test
    fun `focus session blocks apps and sites while unlocked`() {
        assertEquals(Decision.BlockApp(APP), decider.decide(event(APP), isLocked = false, isInFocus = true))
        assertEquals(
            Decision.BlockSite("youtube.com"),
            decider.decide(event(BlockList.CHROME, url = "youtube.com"), isLocked = false, isInFocus = true),
        )
        assertTrue(decider.needsUrlBar(BlockList.CHROME, isLocked = false, isInFocus = true))
    }

    @Test
    fun `focus session ignores a break`() {
        val decision = decider.decide(event(APP), isLocked = true, isOnBreak = true, isInFocus = true)

        assertEquals(Decision.BlockApp(APP), decision)
    }

    @Test
    fun `focus session ignores daily allowance`() {
        val decision = decider.decide(event(APP), isLocked = true, isInFocus = true, remainingMs = { 30 * MINUTE })

        assertEquals(Decision.BlockApp(APP), decision)
    }

    @Test
    fun `settings guard is on during a focus session while unlocked`() {
        val texts = listOf("FocusLock", "Use FocusLock")

        assertTrue(decider.needsScreenTexts("com.android.settings", isLocked = false, isInFocus = true))
        assertEquals(
            Decision.BlockSettings,
            decider.decide(event("com.android.settings", texts = texts), isLocked = false, isInFocus = true),
        )
    }

    @Test
    fun `app with allowance left is allowed and timed`() {
        val decision = decider.decide(event(APP), isLocked = true, remainingMs = { 20 * MINUTE })

        assertEquals(Decision.AllowTimed(APP, 20 * MINUTE), decision)
    }

    @Test
    fun `app with used up allowance hits the limit`() {
        val decision = decider.decide(event(APP), isLocked = true, remainingMs = { 0L })

        assertEquals(Decision.LimitReached(APP), decision)
    }

    @Test
    fun `app without allowance today stays blocked`() {
        val decision = decider.decide(event(APP), isLocked = true, remainingMs = { null })

        assertEquals(Decision.BlockApp(APP), decision)
    }

    @Test
    fun `allowance only matters while locked`() {
        assertEquals(Decision.Allow, decider.decide(event(APP), isLocked = false, remainingMs = { 0L }))
    }

    @Test
    fun `allowance doesn't apply to apps that aren't blocked`() {
        val decision = decider.decide(event("com.whatsapp"), isLocked = true, remainingMs = { 0L })

        assertEquals(Decision.Allow, decision)
    }

    @Test
    fun `timed allow never fires the blocked screen`() {
        val throttle = DecisionThrottle(dev.focuslock.pin.FakeClock())

        assertFalse(throttle.shouldFire(Decision.AllowTimed(APP, MINUTE)))
        assertEquals(null, BlockedReason.describe(Decision.AllowTimed(APP, MINUTE)))
    }

    @Test
    fun `limit reached explains when it resets`() {
        assertEquals(
            "Instagram: daily limit reached. Resets at midnight.",
            BlockedReason.describe(Decision.LimitReached(APP)),
        )
    }

    private companion object {
        const val APP = "com.instagram.android"
        const val MINUTE = 60_000L
    }
}
