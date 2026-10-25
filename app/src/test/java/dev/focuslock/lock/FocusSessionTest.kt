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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FocusSessionTest {

    private lateinit var store: FakeLockStateStore
    private lateinit var clock: FakeClock
    private lateinit var setup: FakeSetupChecker
    private lateinit var pinManager: PinManager
    private lateinit var manager: LockManager

    @Before
    fun setUp() {
        store = FakeLockStateStore()
        clock = FakeClock()
        setup = FakeSetupChecker()
        pinManager = PinManager(InMemoryPinStore(), PinHasher(iterations = 1_000), clock, UnconfinedTestDispatcher())
        runBlocking { pinManager.setPin(PIN) }
        manager = LockManager(store, pinManager, setup, clock)
    }

    @Test
    fun `starts for the chosen hours and blocks even when unlocked`() {
        val result = manager.startFocusSession(3)

        val session = FocusSession(clock.now, clock.now + 3 * HOUR_MS)
        assertEquals(FocusResult.Started(session), result)
        assertFalse(store.isLocked)
        assertTrue(manager.isBlocking())
        assertTrue(store.isBlocking(clock.now))
    }

    @Test
    fun `only 1, 2, 3 or 6 hours are accepted`() {
        listOf(0, 4, 5, 7, 12, -1).forEach { hours ->
            assertEquals(FocusResult.InvalidHours, manager.startFocusSession(hours))
        }
        assertNull(store.focusSession)
    }

    @Test
    fun `refused when setup is incomplete`() {
        setup.adminActive = false

        assertEquals(FocusResult.Refused(LockRefusal.SETUP_INCOMPLETE), manager.startFocusSession(1))
        assertNull(store.focusSession)
    }

    @Test
    fun `a second session can't replace the running one`() {
        manager.startFocusSession(6)

        val result = manager.startFocusSession(1)

        assertEquals(FocusResult.AlreadyActive(clock.now + 6 * HOUR_MS), result)
    }

    @Test
    fun `unlock is refused even with the right code, and no attempt is used`() = runTest {
        manager.lock()
        manager.startFocusSession(1)

        val result = manager.unlock(PIN)

        assertEquals(UnlockResult.InFocusSession(clock.now + HOUR_MS), result)
        assertTrue(store.isLocked)
        assertEquals(VerifyResult.Wrong(remaining = 4), pinManager.verify("wrong-code"))
    }

    @Test
    fun `break and remove are refused during a session`() = runTest {
        manager.lock()
        manager.startFocusSession(2)

        assertEquals(BreakResult.InFocusSession(clock.now + 2 * HOUR_MS), manager.startBreak(PIN, 1))
        assertEquals(UnlockResult.InFocusSession(clock.now + 2 * HOUR_MS), manager.removeCompletely(PIN))
        assertTrue(setup.adminActive)
    }

    @Test
    fun `starting a session ends a running break`() = runTest {
        manager.lock()
        manager.startBreak(PIN, 2)

        manager.startFocusSession(1)

        assertNull(store.breakWindow)
        assertNull(manager.activeBreak())
    }

    @Test
    fun `session ends by itself and unlock works again`() = runTest {
        manager.lock()
        manager.startFocusSession(1)

        clock.advance(HOUR_MS)

        assertNull(manager.activeFocusSession())
        assertEquals(UnlockResult.Unlocked, manager.unlock(PIN))
        assertFalse(manager.isBlocking())
    }

    @Test
    fun `moving the clock back neither ends nor stretches the session`() {
        manager.startFocusSession(2)

        clock.now -= 24 * HOUR_MS
        val session = manager.activeFocusSession()

        assertEquals(FocusSession(clock.now, clock.now + 2 * HOUR_MS), session)
    }

    @Test
    fun `devForceUnlock clears the session`() {
        manager.startFocusSession(6)

        manager.devForceUnlock()

        assertNull(manager.activeFocusSession())
    }

    private companion object {
        const val PIN = "test482916"
        const val HOUR_MS = 60 * 60 * 1000L
    }
}
