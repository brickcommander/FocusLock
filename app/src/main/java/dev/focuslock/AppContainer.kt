package dev.focuslock

import android.content.Context
import dev.focuslock.lock.LockManager
import dev.focuslock.lock.LockStateStore
import dev.focuslock.policy.DpmPolicyController
import dev.focuslock.policy.PolicyController
import dev.focuslock.policy.RestrictionSchemaReader

class AppContainer(context: Context) {
    val schemaReader: RestrictionSchemaReader = RestrictionSchemaReader(context)
    val policyController: PolicyController = DpmPolicyController(context, schemaReader)
    val lockStateStore: LockStateStore = LockStateStore(context)
    val lockManager: LockManager = LockManager(policyController, lockStateStore)
}
