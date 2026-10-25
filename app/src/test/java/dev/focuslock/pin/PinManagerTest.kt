package dev.focuslock.pin

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinManagerTest {

    private lateinit var store: InMemoryPinStore
    private lateinit var clock: FakeClock
    private lateinit var manager: PinManager

    @Before
    fun setUp() {
        store = InMemoryPinStore()
        clock = FakeClock()
        manager = PinManager(store, PinHasher(iterations = 1_000), clock, UnconfinedTestDispatcher())
    }

    @Test
    fun `setPin rejects less than 6 or more than 12 digits or non-digits`() = runTest {
        listOf("12345", "1234567890123", "12a456", "", "12 3456", "١٢٣٤٥٦").forEach { bad ->
            val result = manager.setPin(bad)

            assertTrue("expected failure for '$bad'", result.isFailure)
        }
        assertFalse(manager.isSet())
    }

    @Test
    fun `setPin accepts 6 and 12 digits`() = runTest {
        assertTrue(manager.setPin("123456").isSuccess)
        assertTrue(PinManager.isValidFormat("123456789012"))
    }

    @Test
    fun `setPin fails when already set`() = runTest {
        manager.setPin("482916")

        val result = manager.setPin("777777")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(VerifyResult.Ok, manager.verify("482916"))
    }

    @Test
    fun `verify correct pin returns Ok and resets counter`() = runTest {
        manager.setPin("482916")
        manager.verify("000000")
        manager.verify("000000")

        val result = manager.verify("482916")

        assertEquals(VerifyResult.Ok, result)
        assertEquals(0, store.failedAttempts)
        assertEquals(VerifyResult.Wrong(remaining = 4), manager.verify("000000"))
    }

    @Test
    fun `verify wrong pin decrements remaining`() = runTest {
        manager.setPin("482916")

        assertEquals(VerifyResult.Wrong(remaining = 4), manager.verify("000000"))
        assertEquals(VerifyResult.Wrong(remaining = 3), manager.verify("000000"))
        assertEquals(VerifyResult.Wrong(remaining = 2), manager.verify("000000"))
    }

    @Test
    fun `5 wrong pins triggers 1h lockout`() = runTest {
        manager.setPin("482916")
        repeat(4) { manager.verify("000000") }

        val fifth = manager.verify("000000")

        assertEquals(VerifyResult.LockedOut(untilMs = clock.now + 60 * 60 * 1000L), fifth)
    }

    @Test
    fun `verify during lockout returns LockedOut even with correct pin`() = runTest {
        manager.setPin("482916")
        repeat(5) { manager.verify("000000") }
        clock.advance(59 * 60 * 1000L)

        val result = manager.verify("482916")

        assertTrue(result is VerifyResult.LockedOut)
    }

    @Test
    fun `lockout expires after 1h`() = runTest {
        manager.setPin("482916")
        repeat(5) { manager.verify("000000") }
        clock.advance(60 * 60 * 1000L)

        val result = manager.verify("482916")

        assertEquals(VerifyResult.Ok, result)
    }

    @Test
    fun `lockout never lasts more than 1h after the clock goes backwards`() = runTest {
        manager.setPin("482916")
        clock.advance(30L * 24 * 60 * 60 * 1000L)
        repeat(5) { manager.verify("000000") }
        clock.now -= 30L * 24 * 60 * 60 * 1000L

        val expectedUntil = clock.now + 60 * 60 * 1000L

        val during = manager.verify("482916")
        clock.advance(60 * 60 * 1000L)
        val after = manager.verify("482916")

        assertEquals(VerifyResult.LockedOut(untilMs = expectedUntil), during)
        assertEquals(VerifyResult.Ok, after)
    }

    @Test
    fun `plain pin never stored`() = runTest {
        val pin = "482916"

        manager.setPin(pin)
        manager.verify("000000")
        manager.changePin(pin, "731905")

        store.allStoredValues().forEach { value ->
            assertFalse("found plain code in '$value'", value.contains(pin))
            assertFalse("found new plain code in '$value'", value.contains("731905"))
        }
    }

    @Test
    fun `changePin requires correct old pin`() = runTest {
        manager.setPin("482916")

        val wrong = manager.changePin("000000", "731905")

        assertTrue(wrong.isFailure)
        val error = wrong.exceptionOrNull()
        assertTrue(error is WrongPinException)
        assertEquals(VerifyResult.Wrong(remaining = 4), (error as WrongPinException).result)
        assertEquals(VerifyResult.Ok, manager.verify("482916"))
    }

    @Test
    fun `changePin with correct old pin replaces the code`() = runTest {
        manager.setPin("482916")

        val result = manager.changePin("482916", "731905")

        assertTrue(result.isSuccess)
        assertEquals(VerifyResult.Ok, manager.verify("731905"))
        assertTrue(manager.verify("482916") is VerifyResult.Wrong)
    }

    @Test
    fun `verify returns NotSet when no code exists`() = runTest {
        assertEquals(VerifyResult.NotSet, manager.verify("482916"))
    }
}
