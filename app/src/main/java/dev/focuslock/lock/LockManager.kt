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
        refusal()?.let { return LockResult.Refused(it) }
        store.breakWindow = null
        store.isLocked = true
        store.lastAppliedEpochMs = clock.nowMs()
        return LockResult.Locked
    }

    suspend fun unlock(pin: String): UnlockResult {
        activeFocusSession()?.let { return UnlockResult.InFocusSession(it.untilMs) }
        return when (val result = pinManager.verify(pin)) {
            VerifyResult.Ok -> {
                store.isLocked = false
                store.breakWindow = null
                UnlockResult.Unlocked
            }
            else -> UnlockResult.Refused(result)
        }
    }

    /** Pauses blocking for [hours] while staying locked. Needs the friend's code, like unlocking. */
    suspend fun startBreak(pin: String, hours: Int): BreakResult {
        activeFocusSession()?.let { return BreakResult.InFocusSession(it.untilMs) }
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

    fun activeBreak(): BreakWindow? {
        if (activeFocusSession() != null) return null
        return store.breakWindow?.takeIf { store.isLocked && it.isActive(clock.nowMs()) }
    }

    /**
     * Blocks everything for [hours], locked or not. Needs no code (it only makes blocking stricter)
     * but needs a finished setup, otherwise nothing could enforce it.
     */
    fun startFocusSession(hours: Int): FocusResult {
        activeFocusSession()?.let { return FocusResult.AlreadyActive(it.untilMs) }
        if (hours !in FOCUS_HOURS) return FocusResult.InvalidHours
        refusal()?.let { return FocusResult.Refused(it) }
        val now = clock.nowMs()
        val session = FocusSession(startMs = now, untilMs = now + hours * HOUR_MS)
        store.breakWindow = null
        store.focusSession = session
        return FocusResult.Started(session)
    }

    /**
     * The running focus session, or null. If the clock went back before the start, the session is
     * re-anchored to now with its original length, so it can neither end early nor last for days.
     */
    fun activeFocusSession(): FocusSession? {
        val session = store.focusSession ?: return null
        val now = clock.nowMs()
        if (!session.isActive(now)) return null
        if (now < session.startMs) {
            val reanchored = FocusSession(startMs = now, untilMs = now + session.durationMs)
            store.focusSession = reanchored
            return reanchored
        }
        return session
    }

    fun isBlocking(): Boolean = store.isLocked || activeFocusSession() != null

    /** Unlocks first, then gives up the admin, so a failure in between never leaves the phone locked. */
    suspend fun removeCompletely(pin: String): UnlockResult {
        activeFocusSession()?.let { return UnlockResult.InFocusSession(it.untilMs) }
        return when (val result = pinManager.verify(pin)) {
            VerifyResult.Ok -> {
                store.isLocked = false
                store.breakWindow = null
                store.focusSession = null
                setupChecker.deactivateAdmin()
                UnlockResult.Unlocked
            }
            else -> UnlockResult.Refused(result)
        }
    }

    /** Debug safety net: unlocks without a code. Only reachable from a debug-only button. */
    fun devForceUnlock() {
        store.isLocked = false
        store.breakWindow = null
        store.focusSession = null
    }

    private fun refusal(): LockRefusal? {
        val status = setupStatus()
        return when {
            !status.pinSet -> LockRefusal.NO_PIN
            !status.isComplete -> LockRefusal.SETUP_INCOMPLETE
            else -> null
        }
    }

    companion object {
        val BREAK_HOURS = 1..12
        val FOCUS_HOURS = listOf(1, 2, 3, 6)
        private const val HOUR_MS = 60 * 60 * 1000L
    }
}
