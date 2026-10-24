package dev.focuslock.lock

import dev.focuslock.pin.FakeClock
import dev.focuslock.pin.InMemoryPinStore
import dev.focuslock.pin.PinHasher
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.VerifyResult
import dev.focuslock.policy.BlockList
import dev.focuslock.policy.BrowserPolicy
import dev.focuslock.policy.FakePolicyController
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LockManagerTest {

    private lateinit var policy: FakePolicyController
    private lateinit var store: FakeLockStateStore
    private lateinit var pinManager: PinManager
    private lateinit var clock: FakeClock
    private lateinit var manager: LockManager

    @Before
    fun setUp() {
        policy = FakePolicyController().apply { deviceOwner = true }
        store = FakeLockStateStore()
        pinManager = PinManager(
            InMemoryPinStore(),
            PinHasher(iterations = 1_000),
            FakeClock(),
            UnconfinedTestDispatcher(),
        )
        clock = FakeClock()
        manager = LockManager(policy, store, pinManager, clock)
        runBlocking { pinManager.setPin(PIN) }
    }

    private fun unlockWithCorrectPin(): LockReport = runBlocking {
        val result = manager.unlock(PIN)
        assertTrue("expected Unlocked but was $result", result is UnlockResult.Unlocked)
        (result as UnlockResult.Unlocked).report
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

        // neverTouch packages are never suspended; only protectSelf may uninstall-block our own package
        assertFalse(policy.suspendedPackages.contains("dev.focuslock"))
        assertFalse(policy.suspendedPackages.contains("com.android.settings"))
        assertFalse(policy.suspendedPackages.contains("com.android.vending"))
        assertFalse(policy.uninstallBlockedPackages.contains("com.android.settings"))
        assertFalse(policy.uninstallBlockedPackages.contains("com.android.vending"))
        assertFalse(report.applied.contains("dev.focuslock"))
        assertTrue(policy.calls.none { it.startsWith("setAppsSuspended") && it.contains("dev.focuslock,") })

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

        val report = unlockWithCorrectPin()

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

    @Test
    fun `lock applies browser policy to Chrome when installed`() {
        policy.installedPackages.add(BlockList.CHROME)

        val report = manager.lock()

        assertEquals(BrowserPolicy.locked(), policy.browserPolicies[BlockList.CHROME])
        assertEquals(listOf(BlockList.CHROME), report.browserPolicyApplied)
        assertFalse(report.skippedNotInstalled.contains(BlockList.CHROME))
    }

    @Test
    fun `lock skips browser policy when Chrome not installed`() {
        val report = manager.lock()

        assertFalse(policy.browserPolicies.containsKey(BlockList.CHROME))
        assertTrue(report.browserPolicyApplied.isEmpty())
        assertTrue(report.skippedNotInstalled.contains(BlockList.CHROME))
    }

    @Test
    fun `unlock clears browser policy with null`() {
        policy.installedPackages.add(BlockList.CHROME)
        manager.lock()

        unlockWithCorrectPin()

        assertTrue(policy.browserPolicies.containsKey(BlockList.CHROME))
        assertNull(policy.browserPolicies[BlockList.CHROME])
        assertTrue(policy.calls.contains("setBrowserPolicy:${BlockList.CHROME}=null"))
    }

    @Test
    fun `lock adds every user restriction in BlockList`() {
        manager.lock()

        assertEquals(BlockList.userRestrictions.toSet(), policy.userRestrictions)
    }

    @Test
    fun `lock protects self (uninstall block + control disabled)`() {
        manager.lock()

        assertTrue(policy.uninstallBlockedPackages.contains(BlockList.SELF))
        assertTrue(policy.controlDisabledForSelf)
    }

    @Test
    fun `unlock removes every restriction and self protection`() {
        policy.installedPackages.add("dev.focuslock.dummytarget")
        manager.lock()

        unlockWithCorrectPin()

        assertTrue(policy.userRestrictions.isEmpty())
        assertFalse(policy.uninstallBlockedPackages.contains(BlockList.SELF))
        assertFalse(policy.controlDisabledForSelf)
        BlockList.userRestrictions.forEach {
            assertTrue(policy.calls.contains("setUserRestriction:$it=false"))
        }
    }

    @Test
    fun `debug removal unlocks before clearing device owner`() {
        manager.lock()
        policy.calls.clear()

        manager.devRemoveDeviceOwner()

        val clearIndex = policy.calls.indexOf("clearDeviceOwner")
        val unprotectIndex = policy.calls.indexOf("setUninstallBlocked:${BlockList.SELF}=false")
        val lastRestrictionIndex = BlockList.userRestrictions.maxOf {
            policy.calls.indexOf("setUserRestriction:$it=false")
        }
        assertTrue(unprotectIndex >= 0)
        assertTrue(lastRestrictionIndex >= 0)
        assertTrue(clearIndex > unprotectIndex)
        assertTrue(clearIndex > lastRestrictionIndex)
        assertEquals(clearIndex, policy.calls.lastIndex)
        assertFalse(store.isLocked)
    }

    @Test(expected = IllegalStateException::class)
    fun `lock throws when not device owner`() {
        policy.deviceOwner = false
        manager.lock()
    }

    @Test
    fun `lock refuses when no pin set`() {
        val noPinManager = LockManager(
            policy,
            store,
            PinManager(InMemoryPinStore(), PinHasher(iterations = 1_000), FakeClock(), UnconfinedTestDispatcher()),
            clock,
        )

        val error = runCatching { noPinManager.lock() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertFalse(store.isLocked)
        assertTrue(policy.calls.none { it.startsWith("setAppsSuspended") || it.startsWith("setUserRestriction") })
    }

    @Test
    fun `unlock with wrong pin keeps everything locked`() = runTest {
        policy.installedPackages.addAll(listOf("dev.focuslock.dummytarget", BlockList.CHROME))
        manager.lock()
        policy.calls.clear()

        val result = manager.unlock("000000")

        assertEquals(UnlockResult.Refused(VerifyResult.Wrong(remaining = 4)), result)
        assertTrue("controller was called: ${policy.calls}", policy.calls.isEmpty())
        assertTrue(store.isLocked)
        assertTrue(policy.suspendedPackages.contains("dev.focuslock.dummytarget"))
        assertTrue(policy.uninstallBlockedPackages.contains(BlockList.SELF))
        assertEquals(BlockList.userRestrictions.toSet(), policy.userRestrictions)
    }

    @Test
    fun `unlock with correct pin reverses all policies`() {
        policy.installedPackages.addAll(listOf("dev.focuslock.dummytarget", BlockList.CHROME))
        manager.lock()

        val report = unlockWithCorrectPin()

        assertFalse(store.isLocked)
        assertTrue(policy.suspendedPackages.isEmpty())
        assertTrue(policy.uninstallBlockedPackages.isEmpty())
        assertTrue(policy.userRestrictions.isEmpty())
        assertFalse(policy.controlDisabledForSelf)
        assertNull(policy.browserPolicies[BlockList.CHROME])
        assertTrue(report.applied.contains("dev.focuslock.dummytarget"))
    }

    @Test
    fun `reapply does nothing when unlocked`() {
        policy.installedPackages.add("dev.focuslock.dummytarget")

        val report = manager.reapplyIfLocked()

        assertNull(report)
        assertTrue("controller was called: ${policy.calls}", policy.calls.isEmpty())
        assertFalse(store.isLocked)
    }

    @Test
    fun `reapply suspends a newly installed blocked app`() {
        policy.installedPackages.add("dev.focuslock.dummytarget")
        manager.lock()
        assertFalse(policy.suspendedPackages.contains("com.instagram.android"))
        policy.installedPackages.add("com.instagram.android")

        val report = manager.reapplyIfLocked()

        assertTrue(policy.suspendedPackages.contains("com.instagram.android"))
        assertTrue(policy.uninstallBlockedPackages.contains("com.instagram.android"))
        assertTrue(report!!.applied.contains("com.instagram.android"))
        assertTrue(store.isLocked)
    }

    @Test
    fun `reapply skips without device owner and keeps locked state`() {
        manager.lock()
        policy.deviceOwner = false
        policy.calls.clear()

        val report = manager.reapplyIfLocked()

        assertNull(report)
        assertTrue(store.isLocked)
        assertEquals(listOf("isDeviceOwner"), policy.calls)
    }

    @Test
    fun `lock and reapply record last applied time`() {
        clock.now = 5_000L
        manager.lock()
        assertEquals(5_000L, store.lastAppliedEpochMs)

        clock.now = 9_000L
        manager.reapplyIfLocked()

        assertEquals(9_000L, store.lastAppliedEpochMs)
    }

    private companion object {
        const val PIN = "482916"
    }
}
