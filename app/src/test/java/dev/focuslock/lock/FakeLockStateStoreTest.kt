package dev.focuslock.lock

import org.junit.Assert.assertEquals
import org.junit.Test

class FakeLockStateStoreTest {

    @Test
    fun `listener is notified on isLocked change`() {
        val store = FakeLockStateStore()
        val heard = mutableListOf<Boolean>()
        store.addListener { heard.add(it) }

        store.isLocked = true
        store.isLocked = false

        assertEquals(listOf(true, false), heard)
    }

    @Test
    fun `removed listener is not notified`() {
        val store = FakeLockStateStore()
        val heard = mutableListOf<Boolean>()
        val listener: (Boolean) -> Unit = { heard.add(it) }
        store.addListener(listener)

        store.removeListener(listener)
        store.isLocked = true

        assertEquals(emptyList<Boolean>(), heard)
    }
}
