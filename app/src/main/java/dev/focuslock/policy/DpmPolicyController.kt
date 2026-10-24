package dev.focuslock.policy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import dev.focuslock.admin.FocusAdminReceiver

class DpmPolicyController(private val context: Context) : PolicyController {

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val admin = ComponentName(context, FocusAdminReceiver::class.java)

    override fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    @Suppress("DEPRECATION")
    override fun clearDeviceOwner() {
        dpm.clearDeviceOwnerApp(context.packageName)
    }
}
