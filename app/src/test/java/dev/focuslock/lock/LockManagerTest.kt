package dev.focuslock.lock

import dev.focuslock.policy.BlockList
import dev.focuslock.policy.FakePolicyController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LockManagerTest {

    private lateinit var policy: FakePolicyController
    private lateinit var store: FakeLockStateStore
    private lateinit var manager: LockManager

    @Before
    fun setUp() {
        policy = FakePolicyController().apply { deviceOwner = true }
        store = FakeLockStateStore()
        manager = LockManager(policy, store)
    }

    @Test
    fun `lock suspends only installed apps`() {
        policy.installedPackages.addAll(
            listOf("com.google.android.youtube", "dev.focuslock.dummytarget")
        )

        val report = manager.lock()

        assertTrue(policy.suspendedPackages.contains("com.google.android.youtube"))
        assertTrue(policy.suspendedPackages.contains("dev.focuslock.dummytarget"))
        assertEquals(2, report.applied.size)
        assertTrue(store.isLocked)
    }

    @Test
    fun `lock never touches packages in neverTouch`() {
        // Add a neverTouch package as if it were "installed"
        policy.installedPackages.add("dev.focuslock")
        policy.installedPackages.add("com.android.settings")
        policy.installedPackages.add("com.android.vending")
        policy.installedPackages.add("com.google.android.youtube")

        val report = manager.lock()

        // neverTouch packages must NOT appear in applied, suspended, or uninstall-blocked
        assertFalse(policy.suspendedPackages.contains("dev.focuslock"))
        assertFalse(policy.suspendedPackages.contains("com.android.settings"))
        assertFalse(policy.suspendedPackages.contains("com.android.vending"))
        assertFalse(policy.uninstallBlockedPackages.contains("dev.focuslock"))

        // YouTube should be affected
        assertTrue(policy.suspendedPackages.contains("com.google.android.youtube"))
        assertTrue(report.applied.contains("com.google.android.youtube"))
    }

    @Test
    fun `lock blocks uninstall for each suspended app`() {
        policy.installedPackages.addAll(
            listOf("com.google.android.youtube", "com.reddit.frontpage")
        )

        manager.lock()

        assertTrue(policy.uninstallBlockedPackages.contains("com.google.android.youtube"))
        assertTrue(policy.uninstallBlockedPackages.contains("com.reddit.frontpage"))
    }

    @Test
    fun `lock reports not-installed packages as skipped`() {
        // Only install one app, rest should be skipped
        policy.installedPackages.add("dev.focuslock.dummytarget")

        val report = manager.lock()

        assertTrue(report.applied.contains("dev.focuslock.dummytarget"))
        assertTrue(report.skippedNotInstalled.contains("com.google.android.youtube"))
        assertTrue(report.skippedNotInstalled.contains("com.reddit.frontpage"))
    }

    @Test
    fun `lock reports packages DPM failed to suspend`() {
        policy.installedPackages.addAll(
            listOf("com.google.android.youtube", "com.reddit.frontpage")
        )
        policy.suspendFailures = setOf("com.reddit.frontpage")

        val report = manager.lock()

        assertTrue(report.applied.contains("com.google.android.youtube"))
        assertTrue(report.failed.contains("com.reddit.frontpage"))
        // Failed packages should NOT have uninstall blocked
        assertFalse(policy.uninstallBlockedPackages.contains("com.reddit.frontpage"))
        assertTrue(policy.uninstallBlockedPackages.contains("com.google.android.youtube"))
    }

    @Test
    fun `unlock unsuspends and unblocks uninstall`() {
        policy.installedPackages.addAll(
            listOf("com.google.android.youtube", "dev.focuslock.dummytarget")
        )

        manager.lock()
        assertTrue(store.isLocked)
        assertTrue(policy.suspendedPackages.contains("com.google.android.youtube"))
        assertTrue(policy.uninstallBlockedPackages.contains("com.google.android.youtube"))

        val report = manager.unlock()

        assertFalse(store.isLocked)
        assertTrue(policy.suspendedPackages.isEmpty())
        assertTrue(policy.uninstallBlockedPackages.isEmpty())
        assertTrue(report.applied.contains("com.google.android.youtube"))
        assertTrue(report.applied.contains("dev.focuslock.dummytarget"))
    }

    @Test
    fun `lock twice is idempotent`() {
        policy.installedPackages.add("dev.focuslock.dummytarget")

        val report1 = manager.lock()
        val report2 = manager.lock()

        // Both should succeed without errors
        assertTrue(report1.applied.contains("dev.focuslock.dummytarget"))
        assertTrue(report2.applied.contains("dev.focuslock.dummytarget"))
        assertTrue(store.isLocked)
    }

    @Test(expected = IllegalStateException::class)
    fun `lock throws when not device owner`() {
        policy.deviceOwner = false
        manager.lock()
    }
}
