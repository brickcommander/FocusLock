package dev.focuslock.lock

import dev.focuslock.pin.Clock
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.VerifyResult
import dev.focuslock.setup.SetupChecker
import dev.focuslock.setup.SetupStatus

class LockManager(
    private val store: LockStateStoreContract,
    private val pinManager: PinManager,
    private val setupChecker: SetupChecker,
    private val clock: Clock,
) {

    fun setupStatus(): SetupStatus = SetupStatus(
        blockerServiceEnabled = setupChecker.isBlockerServiceEnabled(),
        deviceAdminActive = setupChecker.isDeviceAdminActive(),
        pinSet = pinManager.isSet(),
    )

    fun lock(): LockResult {
        val status = setupStatus()
        if (!status.pinSet) return LockResult.Refused(LockRefusal.NO_PIN)
        if (!status.isComplete) return LockResult.Refused(LockRefusal.SETUP_INCOMPLETE)
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

    /** Unlocks first, then gives up the admin, so a failure in between never leaves the phone locked. */
    suspend fun removeCompletely(pin: String): UnlockResult =
        when (val result = pinManager.verify(pin)) {
            VerifyResult.Ok -> {
                store.isLocked = false
                setupChecker.deactivateAdmin()
                UnlockResult.Unlocked
            }
            else -> UnlockResult.Refused(result)
        }

    /** Debug safety net: unlocks without a code. Only reachable from a debug-only button. */
    fun devForceUnlock() {
        store.isLocked = false
    }
}
