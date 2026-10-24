package dev.focuslock.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserPolicyTest {

    @Test
    fun `locked policy blocklist contains all sites`() {
        val policy = BrowserPolicy.locked()

        listOf("youtube.com", "youtu.be", "reddit.com", "redd.it", "instagram.com").forEach {
            assertTrue("missing $it", policy.urlBlocklist.contains(it))
        }
    }

    @Test
    fun `locked policy disables incognito`() {
        val policy = BrowserPolicy.locked()

        assertEquals(1, policy.incognitoModeAvailability)
    }
}
