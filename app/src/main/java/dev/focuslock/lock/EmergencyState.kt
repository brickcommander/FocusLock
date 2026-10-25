package dev.focuslock.lock

/** The current state of an emergency unlock request. */
sealed class EmergencyState {
    /** Waiting [unlockAtMs] to arrive (12 h). */
    data class Countdown(val unlockAtMs: Long) : EmergencyState()
    /** Unlocked temporarily; will re-lock at [relockAtMs] (1 h). */
    data class TemporaryUnlock(val relockAtMs: Long) : EmergencyState()
}

sealed class EmergencyResult {
    data class Started(val unlockAtMs: Long) : EmergencyResult()
    data object NotLocked : EmergencyResult()
    data object InFocusSession : EmergencyResult()
    data object AlreadyPending : EmergencyResult()
}
