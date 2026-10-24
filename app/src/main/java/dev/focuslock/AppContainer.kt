package dev.focuslock

import android.content.Context
import dev.focuslock.lock.LockManager
import dev.focuslock.lock.LockStateStore
import dev.focuslock.policy.DpmPolicyController
import dev.focuslock.policy.PolicyController

class AppContainer(context: Context) {
    val policyController: PolicyController = DpmPolicyController(context)
    val lockStateStore: LockStateStore = LockStateStore(context)
    val lockManager: LockManager = LockManager(policyController, lockStateStore)
}
