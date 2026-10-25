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
    fun `setPin rejects shorter than 7 or longer than 30 or empty`() = runTest {
        listOf("123456", "ab", "", "a".repeat(31)).forEach { bad ->
            val result = manager.setPin(bad)

            assertTrue("expected failure for '$bad'", result.isFailure)
        }
        assertFalse(manager.isSet())
    }

    @Test
    fun `setPin accepts 7 characters and 30 characters including any chars`() = runTest {
        assertTrue(PinManager.isValidFormat("abc1234"))
        assertTrue(PinManager.isValidFormat("a".repeat(30)))
        assertTrue(manager.setPin("hello w0rld!").isSuccess)
    }

    @Test
    fun `setPin fails when already set`() = runTest {
        manager.setPin("test482916")

        val result = manager.setPin("other777")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(VerifyResult.Ok, manager.verify("test482916"))
    }

    @Test
    fun `verify correct pin returns Ok and resets counter`() = runTest {
        manager.setPin("test482916")
        manager.verify("wrong00000")
        manager.verify("wrong00000")

        val result = manager.verify("test482916")

        assertEquals(VerifyResult.Ok, result)
        assertEquals(0, store.failedAttempts)
        assertEquals(VerifyResult.Wrong(remaining = 4), manager.verify("wrong00000"))
    }

    @Test
    fun `verify wrong pin decrements remaining`() = runTest {
        manager.setPin("test482916")

        assertEquals(VerifyResult.Wrong(remaining = 4), manager.verify("wrong00000"))
        assertEquals(VerifyResult.Wrong(remaining = 3), manager.verify("wrong00000"))
        assertEquals(VerifyResult.Wrong(remaining = 2), manager.verify("wrong00000"))
    }

    @Test
    fun `5 wrong pins triggers 1h lockout`() = runTest {
        manager.setPin("test482916")
        repeat(4) { manager.verify("wrong00000") }

        val fifth = manager.verify("wrong00000")

        assertEquals(VerifyResult.LockedOut(untilMs = clock.now + 60 * 60 * 1000L), fifth)
    }

    @Test
    fun `verify during lockout returns LockedOut even with correct pin`() = runTest {
        manager.setPin("test482916")
        repeat(5) { manager.verify("wrong00000") }
        clock.advance(59 * 60 * 1000L)

        val result = manager.verify("test482916")

        assertTrue(result is VerifyResult.LockedOut)
    }

    @Test
    fun `lockout expires after 1h`() = runTest {
        manager.setPin("test482916")
        repeat(5) { manager.verify("wrong00000") }
        clock.advance(60 * 60 * 1000L)

        val result = manager.verify("test482916")

        assertEquals(VerifyResult.Ok, result)
    }

    @Test
    fun `lockout never lasts more than 1h after the clock goes backwards`() = runTest {
        manager.setPin("test482916")
        clock.advance(30L * 24 * 60 * 60 * 1000L)
        repeat(5) { manager.verify("wrong00000") }
        clock.now -= 30L * 24 * 60 * 60 * 1000L

        val expectedUntil = clock.now + 60 * 60 * 1000L

        val during = manager.verify("test482916")
        clock.advance(60 * 60 * 1000L)
        val after = manager.verify("test482916")

        assertEquals(VerifyResult.LockedOut(untilMs = expectedUntil), during)
        assertEquals(VerifyResult.Ok, after)
    }

    @Test
    fun `plain pin never stored`() = runTest {
        val pin = "test482916"

        manager.setPin(pin)
        manager.verify("wrong00000")
        manager.changePin(pin, "new731905")

        store.allStoredValues().forEach { value ->
            assertFalse("found plain code in '$value'", value.contains(pin))
            assertFalse("found new plain code in '$value'", value.contains("new731905"))
        }
    }

    @Test
    fun `changePin requires correct old pin`() = runTest {
        manager.setPin("test482916")

        val wrong = manager.changePin("wrong00000", "new731905")

        assertTrue(wrong.isFailure)
        val error = wrong.exceptionOrNull()
        assertTrue(error is WrongPinException)
        assertEquals(VerifyResult.Wrong(remaining = 4), (error as WrongPinException).result)
        assertEquals(VerifyResult.Ok, manager.verify("test482916"))
    }

    @Test
    fun `changePin with correct old pin replaces the code`() = runTest {
        manager.setPin("test482916")

        val result = manager.changePin("test482916", "new731905")

        assertTrue(result.isSuccess)
        assertEquals(VerifyResult.Ok, manager.verify("new731905"))
        assertTrue(manager.verify("test482916") is VerifyResult.Wrong)
    }

    @Test
    fun `empty code is refused without hashing or using an attempt`() = runTest {
        manager.setPin("test482916")

        assertEquals(VerifyResult.Empty, manager.verify(""))
        assertEquals(0, store.failedAttempts)
        assertEquals(VerifyResult.Wrong(remaining = 4), manager.verify("wrong00000"))
    }

    @Test
    fun `a code set under the old 6 digit rule still verifies`() = runTest {
        val hasher = PinHasher(iterations = 1_000)
        val salt = hasher.newSalt()
        val hash = hasher.hash("123456".toCharArray(), salt)
        store.saveCredential(
            saltB64 = java.util.Base64.getEncoder().encodeToString(salt),
            hashB64 = java.util.Base64.getEncoder().encodeToString(hash),
            iterations = 1_000,
        )

        assertEquals(VerifyResult.Ok, manager.verify("123456"))
    }

    @Test
    fun `verify returns NotSet when no code exists`() = runTest {
        assertEquals(VerifyResult.NotSet, manager.verify("test482916"))
    }
}
