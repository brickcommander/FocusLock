package dev.focuslock.lock

import dev.focuslock.policy.BlockList
import dev.focuslock.policy.PolicyController

class LockManager(
    private val policy: PolicyController,
    private val store: LockStateStoreContract,
) {

    fun lock(): LockReport {
        check(policy.isDeviceOwner()) { "Cannot lock: not Device Owner" }

        val targets = BlockList.apps.filter { it !in BlockList.neverTouch }

        val installed = targets.filter { policy.isInstalled(it) }
        val skipped = targets.filter { !policy.isInstalled(it) }

        val failed = if (installed.isNotEmpty()) {
            policy.setAppsSuspended(installed, true)
        } else {
            emptyList()
        }

        val applied = installed.filter { it !in failed }

        for (pkg in applied) {
            policy.setUninstallBlocked(pkg, true)
        }

        store.isLocked = true

        return LockReport(
            applied = applied,
            skippedNotInstalled = skipped,
            failed = failed,
        )
    }

    fun unlock(): LockReport {
        check(policy.isDeviceOwner()) { "Cannot unlock: not Device Owner" }

        val targets = BlockList.apps.filter { it !in BlockList.neverTouch }

        val installed = targets.filter { policy.isInstalled(it) }
        val skipped = targets.filter { !policy.isInstalled(it) }

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
}
