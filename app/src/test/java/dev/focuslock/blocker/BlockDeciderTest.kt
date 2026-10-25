package dev.focuslock.blocker

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockDeciderTest {

    private val decider = BlockDecider(SettingsGuard())

    private fun event(pkg: String, url: String? = null, texts: List<String> = emptyList()) =
        ScreenEvent(packageName = pkg, urlBarText = url, screenTexts = texts)

    @Test
    fun `unlocked always allows`() {
        val events = listOf(
            event("com.instagram.android"),
            event(BlockList.CHROME, url = "youtube.com"),
            event("com.android.settings"),
        )

        events.forEach { assertEquals(Decision.Allow, decider.decide(it, isLocked = false)) }
    }

    @Test
    fun `blocked app is blocked when locked`() {
        val targets = listOf(
            "com.instagram.android",
            "com.instagram.lite",
            "com.google.android.youtube",
            "com.reddit.frontpage",
        )

        targets.forEach { pkg ->
            assertEquals(Decision.BlockApp(pkg), decider.decide(event(pkg), isLocked = true))
        }
    }

    @Test
    fun `self and play store never blocked`() {
        val self = decider.decide(event(BlockList.SELF), isLocked = true)
        val playStore = decider.decide(event("com.android.vending"), isLocked = true)

        assertEquals(Decision.Allow, self)
        assertEquals(Decision.Allow, playStore)
    }

    @Test
    fun `chrome with blocked host is BlockSite`() {
        val decision = decider.decide(event(BlockList.CHROME, url = "https://m.youtube.com/watch?v=1"), isLocked = true)

        assertEquals(Decision.BlockSite("m.youtube.com"), decision)
    }

    @Test
    fun `chrome with search text is allowed`() {
        val decision = decider.decide(event(BlockList.CHROME, url = "reddit.com alternatives"), isLocked = true)

        assertEquals(Decision.Allow, decision)
    }

    @Test
    fun `chrome with null url bar is allowed`() {
        val decision = decider.decide(event(BlockList.CHROME, url = null), isLocked = true)

        assertEquals(Decision.Allow, decision)
    }

    @Test
    fun `chrome with unrelated host is allowed`() {
        val decision = decider.decide(event(BlockList.CHROME, url = "google.com"), isLocked = true)

        assertEquals(Decision.Allow, decision)
    }

    @Test
    fun `unknown package is allowed`() {
        val decision = decider.decide(event("com.example.notes"), isLocked = true)

        assertEquals(Decision.Allow, decision)
    }
}
