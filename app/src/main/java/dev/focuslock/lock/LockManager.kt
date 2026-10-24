package dev.focuslock.lock

import dev.focuslock.policy.BlockList
import dev.focuslock.policy.BrowserPolicy
import dev.focuslock.policy.PolicyController

class LockManager(
    private val policy: PolicyController,
    private val store: LockStateStoreContract,
) {

    fun lock(): LockReport {
        check(policy.isDeviceOwner()) { "Cannot lock: not Device Owner" }

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

        return LockReport(
            applied = applied,
            skippedNotInstalled = if (chromeInstalled) skipped else skipped + BlockList.CHROME,
            failed = failed,
            browserPolicyApplied = if (chromeInstalled) listOf(BlockList.CHROME) else emptyList(),
        )
    }

    fun unlock(): LockReport {
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

    /** Debug safety net: reverses every policy, then gives up Device Owner. */
    fun devRemoveDeviceOwner() {
        unlock()
        policy.clearDeviceOwner()
    }

    /** Deliberately bypasses the neverTouch guard: only this method may target our own package. */
    private fun protectSelf(enabled: Boolean) {
        policy.setUninstallBlocked(BlockList.SELF, enabled)
        policy.setSelfControlDisabled(enabled)
    }

    private fun blockTargets(): List<String> = BlockList.apps.filter { it !in BlockList.neverTouch }
}
