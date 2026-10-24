package dev.focuslock

import android.content.Context
import dev.focuslock.policy.DpmPolicyController
import dev.focuslock.policy.PolicyController

class AppContainer(context: Context) {
    val policyController: PolicyController = DpmPolicyController(context)
}
