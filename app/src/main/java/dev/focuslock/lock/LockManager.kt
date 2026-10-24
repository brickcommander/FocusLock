package dev.focuslock.lock

import dev.focuslock.policy.BlockList
import dev.focuslock.policy.BrowserPolicy
import dev.focuslock.pin.Clock
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.VerifyResult
import dev.focuslock.policy.PolicyController

class LockManager(
    private val policy: PolicyController,
    private val store: LockStateStoreContract,
    private val pinManager: PinManager,
    private val clock: Clock,
) {

    fun lock(): LockReport {
        check(policy.isDeviceOwner()) { "Cannot lock: not Device Owner" }
        check(pinManager.isSet()) { "Cannot lock: no code set" }
        return applyAll()
    }

    /**
     * Re-runs the lock steps when the stored state says locked. Returns null when nothing ran.
     * Without Device Owner it skips and keeps isLocked, so "set DO again, then Unlock" still works.
     */
    fun reapplyIfLocked(): LockReport? {
        if (!store.isLocked) return null
        if (!policy.isDeviceOwner()) return null
        return applyAll()
    }

    private fun applyAll(): LockReport {
        val (installed, skipped) = blockTargets().partition { policy.isInstalled(it) }

        val failed = if (installed.isNotEmpty()) {
            policy.setAppsSuspended(installed, true)
        } else {
            emptyList()
        }

        val applied = installed.filter { it !in failed }

        for (pkg in applied) {
            policy.setUninstallBlocked(pkg, true)
        }

        val chromeInstalled = policy.isInstalled(BlockList.CHROME)
        if (chromeInstalled) {
            policy.setBrowserPolicy(BlockList.CHROME, BrowserPolicy.locked())
        }

        for (key in BlockList.userRestrictions) {
            policy.setUserRestriction(key, true)
        }

        protectSelf(true)

        store.isLocked = true
        store.lastAppliedEpochMs = clock.nowMs()

        return LockReport(
            applied = applied,
            skippedNotInstalled = if (chromeInstalled) skipped else skipped + BlockList.CHROME,
            failed = failed,
            browserPolicyApplied = if (chromeInstalled) listOf(BlockList.CHROME) else emptyList(),
        )
    }

    suspend fun unlock(pin: String): UnlockResult =
        when (val result = pinManager.verify(pin)) {
            VerifyResult.Ok -> UnlockResult.Unlocked(reverseAll())
            else -> UnlockResult.Refused(result)
        }

    /** Debug safety net: reverses every policy without a code, then gives up Device Owner. */
    fun devRemoveDeviceOwner() {
        reverseAll()
        policy.clearDeviceOwner()
    }

    private fun reverseAll(): LockReport {
        check(policy.isDeviceOwner()) { "Cannot unlock: not Device Owner" }

        protectSelf(false)

        for (key in BlockList.userRestrictions) {
            policy.setUserRestriction(key, false)
        }

        policy.setBrowserPolicy(BlockList.CHROME, null)

        val (installed, skipped) = blockTargets().partition { policy.isInstalled(it) }

        for (pkg in installed) {
            policy.setUninstallBlocked(pkg, false)
        }

        val failed = if (installed.isNotEmpty()) {
            policy.setAppsSuspended(installed, false)
        } else {
            emptyList()
        }

        val applied = installed.filter { it !in failed }

        store.isLocked = false

        return LockReport(
            applied = applied,
            skippedNotInstalled = skipped,
            failed = failed,
        )
    }

    /** Deliberately bypasses the neverTouch guard: only this method may target our own package. */
    private fun protectSelf(enabled: Boolean) {
        policy.setUninstallBlocked(BlockList.SELF, enabled)
        policy.setSelfControlDisabled(enabled)
    }

    private fun blockTargets(): List<String> = BlockList.apps.filter { it !in BlockList.neverTouch }
}
