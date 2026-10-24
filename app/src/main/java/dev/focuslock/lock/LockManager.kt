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

    private fun blockTargets(): List<String> = BlockList.apps.filter { it !in BlockList.neverTouch }
}
