package dev.focuslock.lock

import dev.focuslock.pin.Clock
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.VerifyResult

class LockManager(
    private val store: LockStateStoreContract,
    private val pinManager: PinManager,
    private val clock: Clock,
) {

    fun lock(): LockResult {
        if (!pinManager.isSet()) return LockResult.Refused(LockRefusal.NO_PIN)
        store.isLocked = true
        store.lastAppliedEpochMs = clock.nowMs()
        return LockResult.Locked
    }

    suspend fun unlock(pin: String): UnlockResult =
        when (val result = pinManager.verify(pin)) {
            VerifyResult.Ok -> {
                store.isLocked = false
                UnlockResult.Unlocked
            }
            else -> UnlockResult.Refused(result)
        }

    /** Debug safety net: unlocks without a code. Only reachable from a debug-only button. */
    fun devForceUnlock() {
        store.isLocked = false
    }
}
