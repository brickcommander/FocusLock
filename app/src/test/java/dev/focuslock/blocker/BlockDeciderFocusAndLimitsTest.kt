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
    fun `blocked app without a limit stays blocked`() {
        val decision = decider.decide(event(APP), isLocked = true, remainingMs = { null })

        assertEquals(Decision.BlockApp(APP), decision)
    }

    @Test
    fun `limits apply while unlocked`() {
        assertEquals(Decision.LimitReached(APP), decider.decide(event(APP), isLocked = false, remainingMs = { 0L }))
        assertEquals(
            Decision.AllowTimed(APP, MINUTE),
            decider.decide(event(APP), isLocked = false, remainingMs = { MINUTE }),
        )
    }

    @Test
    fun `limits apply to apps that aren't on the block list`() {
        assertEquals(Decision.LimitReached(OTHER), decider.decide(event(OTHER), isLocked = true, remainingMs = { 0L }))
        assertEquals(Decision.LimitReached(OTHER), decider.decide(event(OTHER), isLocked = false, remainingMs = { 0L }))
    }

    @Test
    fun `unlocked app without a limit is allowed`() {
        assertEquals(Decision.Allow, decider.decide(event(APP), isLocked = false, remainingMs = { null }))
    }

    @Test
    fun `a break pauses limits and doesn't count time`() {
        val usedUp = decider.decide(event(OTHER), isLocked = true, isOnBreak = true, remainingMs = { 0L })
        val timeLeft = decider.decide(event(OTHER), isLocked = true, isOnBreak = true, remainingMs = { MINUTE })

        assertEquals(Decision.Allow, usedUp)
        assertEquals(Decision.Allow, timeLeft)
    }

    @Test
    fun `focus session blocks limited apps that aren't on the block list`() {
        val decision = decider.decide(event(OTHER), isLocked = false, isInFocus = true, remainingMs = { 30 * MINUTE })

        assertEquals(Decision.BlockApp(OTHER), decision)
    }

    @Test
    fun `a limited browser still blocks sites while locked`() {
        val decision = decider.decide(
            event(BlockList.CHROME, url = "m.youtube.com/watch"),
            isLocked = true,
            remainingMs = { 10 * MINUTE },
        )

        assertEquals(Decision.BlockSite("m.youtube.com"), decision)
    }

    @Test
    fun `settings guard is on while unlocked when limits exist`() {
        val texts = listOf("FocusLock", "Use FocusLock")

        assertTrue(decider.needsScreenTexts(SETTINGS, isLocked = false, hasLimits = true))
        assertFalse(decider.needsScreenTexts(SETTINGS, isLocked = false, hasLimits = false))
        assertEquals(Decision.BlockSettings, decider.decide(event(SETTINGS, texts = texts), isLocked = false, hasLimits = true))
        assertEquals(Decision.Allow, decider.decide(event(SETTINGS, texts = texts), isLocked = false, hasLimits = false))
    }

    @Test
    fun `settings and never-blocked apps can't be limited`() {
        assertEquals(Decision.Allow, decider.decide(event(SETTINGS), isLocked = false, remainingMs = { 0L }, hasLimits = true))
        assertEquals(Decision.Allow, decider.decide(event(BlockList.SELF), isLocked = false, remainingMs = { 0L }))
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
            "Instagram: no time left today. Resets at midnight.",
            BlockedReason.describe(Decision.LimitReached(APP)),
        )
    }

    private companion object {
        const val APP = "com.instagram.android"
        const val OTHER = "com.whatsapp"
        const val SETTINGS = "com.android.settings"
        const val MINUTE = 60_000L
    }
}
