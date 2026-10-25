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
        store.breakWindow = null
        store.isLocked = true
        store.lastAppliedEpochMs = clock.nowMs()
        return LockResult.Locked
    }

    suspend fun unlock(pin: String): UnlockResult =
        when (val result = pinManager.verify(pin)) {
            VerifyResult.Ok -> {
                store.isLocked = false
                store.breakWindow = null
                UnlockResult.Unlocked
            }
            else -> UnlockResult.Refused(result)
        }

    /** Pauses blocking for [hours] while staying locked. Needs the friend's code, like unlocking. */
    suspend fun startBreak(pin: String, hours: Int): BreakResult {
        if (!store.isLocked) return BreakResult.NotLocked
        if (hours !in BREAK_HOURS) return BreakResult.InvalidHours
        return when (val result = pinManager.verify(pin)) {
            VerifyResult.Ok -> {
                val now = clock.nowMs()
                val window = BreakWindow(startMs = now, untilMs = now + hours * HOUR_MS)
                store.breakWindow = window
                BreakResult.Started(window)
            }
            else -> BreakResult.Refused(result)
        }
    }

    /** Ending a break early only makes blocking stricter, so it needs no code. */
    fun endBreak() {
        store.breakWindow = null
    }

    fun activeBreak(): BreakWindow? =
        store.breakWindow?.takeIf { store.isLocked && it.isActive(clock.nowMs()) }

    /** Unlocks first, then gives up the admin, so a failure in between never leaves the phone locked. */
    suspend fun removeCompletely(pin: String): UnlockResult =
        when (val result = pinManager.verify(pin)) {
            VerifyResult.Ok -> {
                store.isLocked = false
                store.breakWindow = null
                setupChecker.deactivateAdmin()
                UnlockResult.Unlocked
            }
            else -> UnlockResult.Refused(result)
        }

    /** Debug safety net: unlocks without a code. Only reachable from a debug-only button. */
    fun devForceUnlock() {
        store.isLocked = false
        store.breakWindow = null
    }

    companion object {
        val BREAK_HOURS = 1..12
        private const val HOUR_MS = 60 * 60 * 1000L
    }
}
