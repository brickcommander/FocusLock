package dev.focuslock.lock

/**
 * In-memory replacement for LockStateStore used in JVM unit tests
 * (no Android Context needed). Like SharedPreferences, listeners only hear real changes.
 */
class FakeLockStateStore : LockStateStoreContract {
    private val listeners = mutableSetOf<(Boolean) -> Unit>()

    override var isLocked: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            listeners.toList().forEach { it(value) }
        }

    override var lastAppliedEpochMs: Long = 0L

    override fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
    }

    override fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }
}
