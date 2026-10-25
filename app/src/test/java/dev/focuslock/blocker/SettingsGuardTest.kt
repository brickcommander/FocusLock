package dev.focuslock.blocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Screen texts below are copied from the AOSP emulator spike (see SettingsGuard.kt). */
class SettingsGuardTest {

    private val guard = SettingsGuard()

    @Test
    fun `G1 app info for FocusLock is dangerous`() {
        val texts = listOf(
            "FocusLock", "Archive", "Uninstall", "Force stop", "Notifications", "Off", "Permissions",
            "No permissions requested", "Storage & cache", "39.56 MB used in internal storage", "Navigate up",
            "App info", "Open",
        )

        assertTrue(guard.isDangerous(texts))
    }

    @Test
    fun `G1 storage page for FocusLock is dangerous`() {
        val texts = listOf(
            "Storage", "Navigate up", "FocusLock", "1.0", "Clear storage", "Clear storage", "Clear cache",
            "Clear cache", "Space used", "App size", "39.45 MB", "User data", "86.02 kB",
        )

        assertTrue(guard.isDangerous(texts))
    }

    @Test
    fun `G1 app info for another app is not dangerous`() {
        val texts = listOf("DummyTarget", "Archive", "Uninstall", "Force stop", "App info", "Open")

        assertFalse(guard.isDangerous(texts))
    }

    @Test
    fun `G2 device admin detail is dangerous`() {
        val texts = listOf(
            "Device admin app", "Navigate up", "FocusLock",
            "This admin app is active and allows the app FocusLock to perform the following operations:",
            "Deactivate this device admin app", "Cancel",
        )

        assertTrue(guard.isDangerous(texts))
    }

    @Test
    fun `G2 deactivate and uninstall screen is dangerous`() {
        val texts = listOf(
            "Device admin app", "Navigate up", "FocusLock",
            "This admin app is active and allows the app FocusLock to perform the following operations:",
            "Deactivate & uninstall", "Cancel",
        )

        assertTrue(guard.isDangerous(texts))
    }

    @Test
    fun `G3 accessibility detail is dangerous`() {
        val texts = listOf(
            "FocusLock", "Navigate up", "Use FocusLock", "Options", "FocusLock shortcut", "Off",
            "Shortcut settings", "App info",
            "FocusLock uses this only to detect when a blocked app or website opens. Nothing is stored or sent anywhere.",
        )

        assertTrue(guard.isDangerous(texts))
    }

    @Test
    fun `G3 turn off dialog is dangerous`() {
        val texts = listOf("Turn off FocusLock?", "Keep on", "Turn off")

        assertTrue(guard.isDangerous(texts))
    }

    @Test
    fun `G4 date and time is dangerous`() {
        val texts = listOf(
            "Date & time", "Navigate up", "Automatic date and time",
            "Set automatically using your network and wireless signals", "Date", "October 25, 2026", "Time",
            "3:22 PM", "Time zone", "Automatic time zone",
        )

        assertTrue(guard.isDangerous(texts))
    }

    @Test
    fun `G1 storage page as seen by the service is dangerous`() {
        val texts = listOf(
            "Storage", "Navigate up", "Storage", "FocusLock", "1.0", "Space used", "App size", "39.45 MB",
            "User data", "81.92 kB", "Cache", "32.77 kB", "Total", "39.56 MB",
        )

        assertTrue(guard.isDangerous(texts))
    }

    @Test
    fun `G2 device admin detail as seen by the service is dangerous`() {
        val texts = listOf(
            "Device admin app", "Navigate up", "Device admin app", "FocusLock",
            "This admin app is active and allows the app FocusLock to perform the following operations:",
        )

        assertTrue(guard.isDangerous(texts))
    }

    @Test
    fun `G4 date and time as seen by the service is dangerous`() {
        val texts = listOf(
            "Date & time", "Navigate up", "Date & time", "Date", "October 25, 2026", "Time", "3:29 PM", "Time zone",
            "Use location for time zone", "Time zone", "GMT+05:30 India Standard Time", "12-hour / 24-hour format",
            "Use 24-hour format", "1:00 PM", "Notifications", "Time zone change",
        )

        assertTrue(guard.isDangerous(texts))
    }

    @Test
    fun `device admin activation screen stays allowed for setup`() {
        val texts = listOf(
            "Device admin app", "Navigate up", "FocusLock", "Stops FocusLock from being uninstalled while locked.",
            "Activating this admin app will allow the app FocusLock to perform the following operations:",
        )

        assertFalse(guard.isDangerous(texts))
    }

    @Test
    fun `settings home is not dangerous`() {
        val texts = listOf(
            "Search Settings", "Apps", "Storage", "42% used - 9.35 GB free", "Battery", "System",
            "Languages, gestures, time", "Accessibility",
        )

        assertFalse(guard.isDangerous(texts))
    }

    @Test
    fun `apps list containing FocusLock is not dangerous`() {
        val texts = listOf(
            "Clock", "65.54 kB", "DummyTarget", "2.60 MB", "Files", "213 kB", "FocusLock", "39.57 MB", "Gboard",
            "14.15 MB", "Navigate up", "All apps", "Search", "More options",
        )

        assertFalse(guard.isDangerous(texts))
    }

    @Test
    fun `accessibility main list is not dangerous`() {
        val texts = listOf(
            "Accessibility", "Navigate up", "Downloaded apps", "FocusLock", "On", "Screen reader", "TalkBack",
            "Off / Use a screen reader to hear items spoken aloud", "Display", "Display size & text",
        )

        assertFalse(guard.isDangerous(texts))
    }

    @Test
    fun `device admin list is not dangerous`() {
        val texts = listOf("Device admin", "Navigate up", "Find Hub", "Allow Find Hub to lock or erase a lost device", "FocusLock")

        assertFalse(guard.isDangerous(texts))
    }

    @Test
    fun `system page listing date and time is not dangerous`() {
        val texts = listOf("System", "Languages", "Keyboard", "Date & time", "GMT+05:30 India Standard Time")

        assertFalse(guard.isDangerous(texts))
    }

    @Test
    fun `matching is case-insensitive`() {
        val texts = listOf("  focuslock ", "FORCE STOP")

        assertTrue(guard.isDangerous(texts))
    }

    @Test
    fun `text that only contains a needle does not match`() {
        val texts = listOf("About FocusLock", "Force stop all apps")

        assertFalse(guard.isDangerous(texts))
    }

    @Test
    fun `empty screen is not dangerous`() {
        assertFalse(guard.isDangerous(emptyList()))
    }
}
