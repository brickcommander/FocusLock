package dev.focuslock.lock

import dev.focuslock.pin.FakeClock
import dev.focuslock.pin.InMemoryPinStore
import dev.focuslock.pin.PinHasher
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.VerifyResult
import dev.focuslock.setup.FakeSetupChecker
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LockManagerTest {

    private lateinit var store: FakeLockStateStore
    private lateinit var clock: FakeClock
    private lateinit var setup: FakeSetupChecker
    private lateinit var manager: LockManager

    @Before
    fun setUp() {
        store = FakeLockStateStore()
        clock = FakeClock()
        setup = FakeSetupChecker()
        val pinManager = newPinManager()
        runBlocking { pinManager.setPin(PIN) }
        manager = LockManager(store, pinManager, setup, clock)
    }

    private fun newPinManager(): PinManager =
        PinManager(InMemoryPinStore(), PinHasher(iterations = 1_000), FakeClock(), UnconfinedTestDispatcher())

    @Test
    fun `lock refuses when no pin set`() {
        val noPinManager = LockManager(store, newPinManager(), setup, clock)

        val result = noPinManager.lock()

        assertEquals(LockResult.Refused(LockRefusal.NO_PIN), result)
        assertFalse(store.isLocked)
        assertEquals(0L, store.lastAppliedEpochMs)
    }

    @Test
    fun `lock refuses when service disabled`() {
        setup.serviceEnabled = false

        val result = manager.lock()

        assertEquals(LockResult.Refused(LockRefusal.SETUP_INCOMPLETE), result)
        assertFalse(store.isLocked)
    }

    @Test
    fun `lock refuses when admin inactive`() {
        setup.adminActive = false

        val result = manager.lock()

        assertEquals(LockResult.Refused(LockRefusal.SETUP_INCOMPLETE), result)
        assertFalse(store.isLocked)
    }

    @Test
    fun `lock succeeds when setup complete`() {
        val result = manager.lock()

        assertEquals(LockResult.Locked, result)
        assertTrue(manager.setupStatus().isComplete)
        assertTrue(store.isLocked)
    }

    @Test
    fun `lock sets isLocked and lastApplied`() {
        clock.now = 5_000L

        val result = manager.lock()

        assertEquals(LockResult.Locked, result)
        assertTrue(store.isLocked)
        assertEquals(5_000L, store.lastAppliedEpochMs)
    }

    @Test
    fun `unlock with wrong pin keeps locked`() = runTest {
        manager.lock()

        val result = manager.unlock("000000")

        assertEquals(UnlockResult.Refused(VerifyResult.Wrong(remaining = 4)), result)
        assertTrue(store.isLocked)
    }

    @Test
    fun `unlock with correct pin clears isLocked`() = runTest {
        manager.lock()

        val result = manager.unlock(PIN)

        assertEquals(UnlockResult.Unlocked, result)
        assertFalse(store.isLocked)
    }

    @Test
    fun `lock twice is idempotent`() {
        clock.now = 1_000L
        manager.lock()
        clock.now = 2_000L

        val result = manager.lock()

        assertEquals(LockResult.Locked, result)
        assertTrue(store.isLocked)
        assertEquals(2_000L, store.lastAppliedEpochMs)
    }

    @Test
    fun `removeCompletely with wrong pin does nothing`() = runTest {
        manager.lock()

        val result = manager.removeCompletely("000000")

        assertEquals(UnlockResult.Refused(VerifyResult.Wrong(remaining = 4)), result)
        assertTrue(store.isLocked)
        assertEquals(0, setup.deactivateAdminCalls)
        assertTrue(setup.adminActive)
    }

    @Test
    fun `removeCompletely unlocks then deactivates admin`() = runTest {
        var lockedWhenAdminDeactivated: Boolean? = null
        val orderedSetup = FakeSetupChecker(onDeactivateAdmin = { lockedWhenAdminDeactivated = store.isLocked })
        val pinManager = newPinManager().apply { setPin(PIN) }
        val orderedManager = LockManager(store, pinManager, orderedSetup, clock)
        orderedManager.lock()

        val result = orderedManager.removeCompletely(PIN)

        assertEquals(UnlockResult.Unlocked, result)
        assertEquals(false, lockedWhenAdminDeactivated)
        assertEquals(1, orderedSetup.deactivateAdminCalls)
        assertFalse(store.isLocked)
        assertFalse(orderedSetup.adminActive)
    }

    @Test
    fun `startBreak with correct pin opens a window of the chosen hours and stays locked`() = runTest {
        manager.lock()
        clock.now = 10_000L

        val result = manager.startBreak(PIN, hours = 3)

        val expected = BreakWindow(startMs = 10_000L, untilMs = 10_000L + 3 * HOUR_MS)
        assertEquals(BreakResult.Started(expected), result)
        assertEquals(expected, store.breakWindow)
        assertTrue(store.isLocked)
        assertEquals(expected, manager.activeBreak())
    }

    @Test
    fun `startBreak with wrong pin starts nothing`() = runTest {
        manager.lock()

        val result = manager.startBreak("000000", hours = 2)

        assertEquals(BreakResult.Refused(VerifyResult.Wrong(remaining = 4)), result)
        assertEquals(null, store.breakWindow)
    }

    @Test
    fun `startBreak is refused when not locked`() = runTest {
        val result = manager.startBreak(PIN, hours = 2)

        assertEquals(BreakResult.NotLocked, result)
        assertEquals(null, store.breakWindow)
    }

    @Test
    fun `startBreak rejects hours outside the allowed range`() = runTest {
        manager.lock()

        assertEquals(BreakResult.InvalidHours, manager.startBreak(PIN, hours = 0))
        assertEquals(BreakResult.InvalidHours, manager.startBreak(PIN, hours = 13))
        assertEquals(null, store.breakWindow)
    }

    @Test
    fun `break is no longer active after it ends`() = runTest {
        manager.lock()
        clock.now = 0L
        manager.startBreak(PIN, hours = 1)

        clock.now = HOUR_MS

        assertEquals(null, manager.activeBreak())
    }

    @Test
    fun `endBreak clears the break without a code`() = runTest {
        manager.lock()
        manager.startBreak(PIN, hours = 1)

        manager.endBreak()

        assertEquals(null, store.breakWindow)
        assertTrue(store.isLocked)
    }

    @Test
    fun `locking again clears an old break`() {
        store.breakWindow = BreakWindow(startMs = 0L, untilMs = Long.MAX_VALUE)

        manager.lock()

        assertEquals(null, store.breakWindow)
    }

    @Test
    fun `unlock clears the break`() = runTest {
        manager.lock()
        manager.startBreak(PIN, hours = 1)

        manager.unlock(PIN)

        assertEquals(null, store.breakWindow)
    }

    @Test
    fun `devForceUnlock clears isLocked without pin`() {
        manager.lock()

        manager.devForceUnlock()

        assertFalse(store.isLocked)
    }

    private companion object {
        const val PIN = "482916"
        const val HOUR_MS = 60 * 60 * 1000L
    }
}
