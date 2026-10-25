package dev.focuslock.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `brave app itself is allowed when locked`() {
        val decision = decider.decide(event(BlockList.BRAVE, url = "google.com"), isLocked = true)

        assertEquals(Decision.Allow, decision)
    }

    @Test
    fun `brave with blocked host is BlockSite`() {
        val decision = decider.decide(event(BlockList.BRAVE, url = "m.youtube.com"), isLocked = true)

        assertEquals(Decision.BlockSite("m.youtube.com"), decision)
    }

    @Test
    fun `brave url bar is read only when locked`() {
        assertTrue(decider.needsUrlBar(BlockList.BRAVE, isLocked = true))
        assertFalse(decider.needsUrlBar(BlockList.BRAVE, isLocked = false))
    }

    @Test
    fun `dangerous settings screen is BlockSettings when locked`() {
        val decision = decider.decide(event(SETTINGS, texts = APP_INFO_TEXTS), isLocked = true)

        assertEquals(Decision.BlockSettings, decision)
    }

    @Test
    fun `settings guard only applies to settings packages`() {
        val decision = decider.decide(event("com.example.notes", texts = APP_INFO_TEXTS), isLocked = true)

        assertEquals(Decision.Allow, decision)
    }

    @Test
    fun `settings guard ignored when unlocked`() {
        val decision = decider.decide(event(SETTINGS, texts = APP_INFO_TEXTS), isLocked = false)

        assertEquals(Decision.Allow, decision)
    }

    @Test
    fun `safe settings screen is allowed when locked`() {
        val decision = decider.decide(event(SETTINGS, texts = listOf("Network & internet", "Wi‑Fi")), isLocked = true)

        assertEquals(Decision.Allow, decision)
    }

    @Test
    fun `url bar and screen texts are only read when they can matter`() {
        assertTrue(decider.needsUrlBar(BlockList.CHROME, isLocked = true))
        assertFalse(decider.needsUrlBar(BlockList.CHROME, isLocked = false))
        assertFalse(decider.needsUrlBar(SETTINGS, isLocked = true))
        assertTrue(decider.needsScreenTexts(SETTINGS, isLocked = true))
        assertFalse(decider.needsScreenTexts(SETTINGS, isLocked = false))
        assertFalse(decider.needsScreenTexts(BlockList.CHROME, isLocked = true))
    }

    @Test
    fun `app added by the user is blocked`() {
        val rules = BlockRules(apps = setOf("com.zhiliaoapp.musically"), sites = emptySet())

        val decision = decider.decide(event("com.zhiliaoapp.musically"), isLocked = true, rules = rules)

        assertEquals(Decision.BlockApp("com.zhiliaoapp.musically"), decision)
    }

    @Test
    fun `default app removed by the user is allowed`() {
        val rules = BlockRules(apps = emptySet(), sites = BlockList.sites)

        val decision = decider.decide(event("com.instagram.android"), isLocked = true, rules = rules)

        assertEquals(Decision.Allow, decision)
    }

    @Test
    fun `site added by the user is blocked in both browsers`() {
        val rules = BlockRules(apps = emptySet(), sites = setOf("tiktok.com"))

        val chrome = decider.decide(event(BlockList.CHROME, url = "www.tiktok.com"), isLocked = true, rules = rules)
        val brave = decider.decide(event(BlockList.BRAVE, url = "tiktok.com"), isLocked = true, rules = rules)
        val youtube = decider.decide(event(BlockList.CHROME, url = "youtube.com"), isLocked = true, rules = rules)

        assertEquals(Decision.BlockSite("www.tiktok.com"), chrome)
        assertEquals(Decision.BlockSite("tiktok.com"), brave)
        assertEquals(Decision.Allow, youtube)
    }

    @Test
    fun `browser added as an app is blocked completely`() {
        val rules = BlockRules(apps = setOf(BlockList.BRAVE), sites = emptySet())

        val decision = decider.decide(event(BlockList.BRAVE, url = "google.com"), isLocked = true, rules = rules)

        assertEquals(Decision.BlockApp(BlockList.BRAVE), decision)
    }

    @Test
    fun `self, play store and settings in the app list are never blocked as apps`() {
        val rules = BlockRules(apps = setOf(BlockList.SELF, "com.android.vending", SETTINGS), sites = emptySet())

        listOf(BlockList.SELF, "com.android.vending", SETTINGS).forEach { pkg ->
            assertEquals(pkg, Decision.Allow, decider.decide(event(pkg), isLocked = true, rules = rules))
        }
    }

    @Test
    fun `unknown package is allowed`() {
        val decision = decider.decide(event("com.example.notes"), isLocked = true)

        assertEquals(Decision.Allow, decision)
    }

    private companion object {
        const val SETTINGS = "com.android.settings"
        val APP_INFO_TEXTS = listOf("FocusLock", "Archive", "Uninstall", "Force stop", "App info")
    }
}
