package dev.focuslock.lock

/**
 * In-memory replacement for LockStateStore used in JVM unit tests
 * (no Android Context needed). Like SharedPreferences, listeners only hear real changes.
 */
class FakeLockStateStore : LockStateStoreContract {
    private val listeners = mutableSetOf<(Boolean) -> Unit>()
    private val breakListeners = mutableSetOf<(BreakWindow?) -> Unit>()

    override var isLocked: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            listeners.toList().forEach { it(value) }
        }

    override var lastAppliedEpochMs: Long = 0L

    override var breakWindow: BreakWindow? = null
        set(value) {
            if (field == value) return
            field = value
            breakListeners.toList().forEach { it(value) }
        }

    override fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
    }

    override fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }

    override fun addBreakListener(listener: (BreakWindow?) -> Unit) {
        breakListeners.add(listener)
    }

    override fun removeBreakListener(listener: (BreakWindow?) -> Unit) {
        breakListeners.remove(listener)
    }
}
