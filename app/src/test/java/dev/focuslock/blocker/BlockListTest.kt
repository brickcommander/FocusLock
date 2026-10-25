package dev.focuslock.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockListTest {

    @Test
    fun `apps and neverBlock do not overlap`() {
        val overlap = BlockList.apps intersect BlockList.neverBlock

        assertTrue("overlap: $overlap", overlap.isEmpty())
    }

    @Test
    fun `instagram lite is in apps`() {
        assertTrue("com.instagram.lite" in BlockList.apps)
    }

    @Test
    fun `settings and browsers are not blocked as apps`() {
        val pagesOnly = BlockList.settingsPackages + BlockList.browsers.keys

        assertTrue((BlockList.apps intersect pagesOnly).isEmpty())
    }

    @Test
    fun `brave is a browser with its spiked url bar id`() {
        assertEquals(listOf("com.brave.browser:id/url_bar"), BlockList.browsers[BlockList.BRAVE])
    }
}
