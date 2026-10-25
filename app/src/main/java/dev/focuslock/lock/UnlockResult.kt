package dev.focuslock.lock

import dev.focuslock.pin.VerifyResult

sealed class UnlockResult {
    data object Unlocked : UnlockResult()
    data class Refused(val reason: VerifyResult) : UnlockResult()
}

enum class LockRefusal { NO_PIN, SETUP_INCOMPLETE }

sealed class LockResult {
    data object Locked : LockResult()
    data class Refused(val reason: LockRefusal) : LockResult()
}
