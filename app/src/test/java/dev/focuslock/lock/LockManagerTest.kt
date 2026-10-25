package dev.focuslock.lock

import dev.focuslock.pin.FakeClock
import dev.focuslock.pin.InMemoryPinStore
import dev.focuslock.pin.PinHasher
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.VerifyResult
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
    private lateinit var manager: LockManager

    @Before
    fun setUp() {
        store = FakeLockStateStore()
        clock = FakeClock()
        val pinManager = newPinManager()
        runBlocking { pinManager.setPin(PIN) }
        manager = LockManager(store, pinManager, clock)
    }

    private fun newPinManager(): PinManager =
        PinManager(InMemoryPinStore(), PinHasher(iterations = 1_000), FakeClock(), UnconfinedTestDispatcher())

    @Test
    fun `lock refuses when no pin set`() {
        val noPinManager = LockManager(store, newPinManager(), clock)

        val result = noPinManager.lock()

        assertEquals(LockResult.Refused(LockRefusal.NO_PIN), result)
        assertFalse(store.isLocked)
        assertEquals(0L, store.lastAppliedEpochMs)
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
    fun `devForceUnlock clears isLocked without pin`() {
        manager.lock()

        manager.devForceUnlock()

        assertFalse(store.isLocked)
    }

    private companion object {
        const val PIN = "482916"
    }
}
