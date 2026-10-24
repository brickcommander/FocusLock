package dev.focuslock

import android.content.Context
import dev.focuslock.lock.LockManager
import dev.focuslock.lock.LockStateStore
import dev.focuslock.pin.PinHasher
import dev.focuslock.pin.PinManager
import dev.focuslock.pin.PinStore
import dev.focuslock.pin.SystemClock
import dev.focuslock.policy.DpmPolicyController
import dev.focuslock.policy.PolicyController
import dev.focuslock.policy.RestrictionSchemaReader

class AppContainer(context: Context) {
    val schemaReader: RestrictionSchemaReader = RestrictionSchemaReader(context)
    val policyController: PolicyController = DpmPolicyController(context, schemaReader)
    val lockStateStore: LockStateStore = LockStateStore(context)
    val pinManager: PinManager = PinManager(PinStore(context), PinHasher(), SystemClock)
    val lockManager: LockManager = LockManager(policyController, lockStateStore, pinManager)
}
