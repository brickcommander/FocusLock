package dev.focuslock.lock

/**
 * In-memory replacement for LockStateStore used in JVM unit tests
 * (no Android Context needed).
 */
class FakeLockStateStore : LockStateStoreContract {
    override var isLocked: Boolean = false
    override var lastAppliedEpochMs: Long = 0L
}
