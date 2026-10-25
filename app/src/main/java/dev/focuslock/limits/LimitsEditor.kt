package dev.focuslock.limits

import dev.focuslock.blocker.BlockList
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.VerifyResult

sealed class LimitEditResult {
    data object Saved : LimitEditResult()
    data class Refused(val reason: VerifyResult) : LimitEditResult()
    data object InFocusSession : LimitEditResult()
    data object Protected : LimitEditResult()
}

/**
 * Every change to the limits needs the code, locked or not: limits apply in both modes, so an
 * unlocked phone must not be a way around them. Before a code exists there is nothing to protect.
 * No changes during a focus session: removing a limit could unblock an app before it ends.
 */
class LimitsEditor(
    private val store: LimitsStoreContract,
    private val pinManager: PinManager,
    private val isInFocus: () -> Boolean,
    private val protectedPackages: () -> Set<String>,
) {

    suspend fun save(pin: String, pkg: String, limit: DailyLimit): LimitEditResult {
        if (pkg in unlimitable()) return LimitEditResult.Protected
        authorize(pin)?.let { return it }
        store.put(pkg, limit)
        return LimitEditResult.Saved
    }

    suspend fun remove(pin: String, pkg: String): LimitEditResult {
        authorize(pin)?.let { return it }
        store.remove(pkg)
        return LimitEditResult.Saved
    }

    fun unlimitable(): Set<String> = BlockList.neverBlock + BlockList.settingsPackages + protectedPackages()

    private suspend fun authorize(pin: String): LimitEditResult? {
        if (isInFocus()) return LimitEditResult.InFocusSession
        if (!pinManager.isSet()) return null
        return when (val result = pinManager.verify(pin)) {
            VerifyResult.Ok -> null
            else -> LimitEditResult.Refused(result)
        }
    }
}
