package dev.focuslock.pin

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PinHasherTest {

    private val hasher = PinHasher(iterations = 1_000)

    @Test
    fun `same pin and salt produce the same hash`() {
        val salt = hasher.newSalt()

        val first = hasher.hash("482916".toCharArray(), salt)
        val second = hasher.hash("482916".toCharArray(), salt)

        assertArrayEquals(first, second)
    }

    @Test
    fun `different salt produces a different hash`() {
        val first = hasher.hash("482916".toCharArray(), hasher.newSalt())
        val second = hasher.hash("482916".toCharArray(), hasher.newSalt())

        assertFalse(first.contentEquals(second))
    }

    @Test
    fun `hash is 256 bits and salt is 16 bytes`() {
        val salt = hasher.newSalt()

        val hash = hasher.hash("482916".toCharArray(), salt)

        assertEquals(16, salt.size)
        assertEquals(32, hash.size)
    }

    @Test
    fun `default iterations is 120000`() {
        assertEquals(120_000, PinHasher().iterations)
    }
}
