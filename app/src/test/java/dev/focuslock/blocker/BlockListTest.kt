package dev.focuslock.blocker

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
    fun `settings and chrome are not blocked as apps`() {
        val pagesOnly = BlockList.settingsPackages + BlockList.CHROME

        assertTrue((BlockList.apps intersect pagesOnly).isEmpty())
    }
}
