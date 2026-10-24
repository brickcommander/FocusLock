package dev.focuslock.lock

import dev.focuslock.pin.VerifyResult

sealed class UnlockResult {
    data class Unlocked(val report: LockReport) : UnlockResult()
    data class Refused(val reason: VerifyResult) : UnlockResult()
}
