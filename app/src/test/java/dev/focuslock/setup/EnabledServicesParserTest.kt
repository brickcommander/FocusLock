package dev.focuslock.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnabledServicesParserTest {

    private fun isOurs(setting: String?) = EnabledServicesParser.isEnabled(setting, PKG, CLS)

    @Test
    fun `empty string is not enabled`() {
        assertFalse(isOurs(""))
    }

    @Test
    fun `null is not enabled`() {
        assertFalse(isOurs(null))
    }

    @Test
    fun `single full entry is enabled`() {
        assertTrue(isOurs("$PKG/$CLS"))
    }

    @Test
    fun `entry among several others is enabled`() {
        val setting = "com.google.android.marvin.talkback/.TalkBackService:$PKG/$CLS:com.example/.Other"

        assertTrue(isOurs(setting))
    }

    @Test
    fun `short-form component is enabled`() {
        assertTrue(isOurs("dev.focuslock/.blocker.FocusAccessibilityService"))
    }

    @Test
    fun `different package with same class name is not enabled`() {
        assertFalse(isOurs("com.evil/$CLS"))
        assertFalse(isOurs("com.evil/.blocker.FocusAccessibilityService"))
    }

    @Test
    fun `other services only is not enabled`() {
        assertFalse(isOurs("com.google.android.marvin.talkback/.TalkBackService"))
    }

    @Test
    fun `quoted empty value written by adb is not enabled`() {
        assertFalse(isOurs("\"\""))
    }

    private companion object {
        const val PKG = "dev.focuslock"
        const val CLS = "dev.focuslock.blocker.FocusAccessibilityService"
    }
}
