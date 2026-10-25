package dev.focuslock.lock

import dev.focuslock.pin.VerifyResult

sealed class UnlockResult {
    data object Unlocked : UnlockResult()
    data class Refused(val reason: VerifyResult) : UnlockResult()

    /** A focus session can't be ended early, not even with the code. */
    data class InFocusSession(val untilMs: Long) : UnlockResult()
}

enum class LockRefusal { NO_PIN, SETUP_INCOMPLETE }

sealed class LockResult {
    data object Locked : LockResult()
    data class Refused(val reason: LockRefusal) : LockResult()
}

sealed class BreakResult {
    data class Started(val window: BreakWindow) : BreakResult()
    data object NotLocked : BreakResult()
    data object InvalidHours : BreakResult()
    data class Refused(val reason: VerifyResult) : BreakResult()
    data class InFocusSession(val untilMs: Long) : BreakResult()
}

sealed class FocusResult {
    data class Started(val session: FocusSession) : FocusResult()
    data object InvalidHours : FocusResult()
    data class Refused(val reason: LockRefusal) : FocusResult()
    data class AlreadyActive(val untilMs: Long) : FocusResult()
}
