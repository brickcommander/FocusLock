package dev.focuslock.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlockedReasonTest {

    @Test
    fun `known blocked app shows its friendly name`() {
        val text = BlockedReason.describe(Decision.BlockApp("com.instagram.lite"))

        assertEquals("Instagram Lite", text)
    }

    @Test
    fun `every blocked app has a friendly name`() {
        BlockList.apps.forEach { pkg ->
            val text = BlockedReason.describe(Decision.BlockApp(pkg))
            assertNotEquals("no friendly name for $pkg", pkg, text)
        }
    }

    @Test
    fun `app added by the user shows its installed label`() {
        val text = BlockedReason.describe(Decision.BlockApp("com.zhiliaoapp.musically")) { "TikTok" }

        assertEquals("TikTok", text)
    }

    @Test
    fun `unknown app falls back to its package name`() {
        val text = BlockedReason.describe(Decision.BlockApp("com.example.other"))

        assertEquals("com.example.other", text)
    }

    @Test
    fun `blocked site shows the host`() {
        val text = BlockedReason.describe(Decision.BlockSite("m.youtube.com"))

        assertEquals("m.youtube.com", text)
    }

    @Test
    fun `blocked settings screen has a generic label`() {
        val text = BlockedReason.describe(Decision.BlockSettings)

        assertEquals(BlockedReason.SETTINGS_SCREEN, text)
    }

    @Test
    fun `allow has nothing to show`() {
        assertNull(BlockedReason.describe(Decision.Allow))
    }
}
