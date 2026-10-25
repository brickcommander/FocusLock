package dev.focuslock.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostMatcherTest {

    @Test
    fun `extracts host from full url`() {
        val host = HostMatcher.extractHost("https://m.youtube.com/watch?v=1")

        assertEquals("m.youtube.com", host)
    }

    @Test
    fun `extracts host from bare domain`() {
        val host = HostMatcher.extractHost("youtube.com")

        assertEquals("youtube.com", host)
    }

    @Test
    fun `handles port, path, query, uppercase, trailing dot`() {
        val inputs = listOf(
            "http://YouTube.com:8080/feed",
            "youtube.com/shorts/abc",
            "youtube.com?x=1",
            "youtube.com#top",
            "YOUTUBE.COM.",
            "  https://youtube.com.:443/  ",
        )

        val hosts = inputs.map { HostMatcher.extractHost(it) }

        hosts.forEachIndexed { i, host -> assertEquals("input '${inputs[i]}'", "youtube.com", host) }
    }

    @Test
    fun `search text with spaces returns null`() {
        val host = HostMatcher.extractHost("reddit.com alternatives")

        assertNull(host)
    }

    @Test
    fun `single word returns null`() {
        val host = HostMatcher.extractHost("youtube")

        assertNull(host)
    }

    @Test
    fun `chrome url bar formats seen in the spike`() {
        val seen = mapOf(
            "youtube.com" to "youtube.com",
            "m.youtube.com" to "m.youtube.com",
            "https://reddit.com" to "reddit.com",
            "reddit.com" to "reddit.com",
        )

        seen.forEach { (text, host) -> assertEquals(text, host, HostMatcher.extractHost(text)) }
    }

    @Test
    fun `chrome empty bar hint returns null`() {
        val host = HostMatcher.extractHost("Search Google or type URL")

        assertNull(host)
    }

    @Test
    fun `blank text returns null`() {
        assertNull(HostMatcher.extractHost(""))
        assertNull(HostMatcher.extractHost("   "))
    }

    @Test
    fun `subdomain is blocked`() {
        assertTrue(HostMatcher.isBlockedHost("old.reddit.com"))
        assertTrue(HostMatcher.isBlockedHost("www.instagram.com"))
        assertTrue(HostMatcher.isBlockedHost("m.youtube.com"))
    }

    @Test
    fun `every listed site is blocked exactly`() {
        BlockList.sites.forEach { assertTrue(it, HostMatcher.isBlockedHost(it)) }
    }

    /**
     * Documented result: only a real suffix match blocks. "youtube.com.evil.io" ends with
     * ".evil.io", so it is NOT blocked (it is not YouTube).
     */
    @Test
    fun `lookalike is not blocked`() {
        assertFalse(HostMatcher.isBlockedHost("notyoutube.com"))
        assertFalse(HostMatcher.isBlockedHost("youtube.com.evil.io"))
        assertFalse(HostMatcher.isBlockedHost("myreddit.com"))
    }

    @Test
    fun `unrelated host is not blocked`() {
        assertFalse(HostMatcher.isBlockedHost("google.com"))
        assertFalse(HostMatcher.isBlockedHost("wikipedia.org"))
    }
}
