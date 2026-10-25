package dev.focuslock.setup

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import dev.focuslock.admin.FocusAdminReceiver
import dev.focuslock.blocker.FocusAccessibilityService

class AndroidSetupChecker(private val context: Context) : SetupChecker {

    private val dpm = context.getSystemService(DevicePolicyManager::class.java)

    val adminComponent = ComponentName(context, FocusAdminReceiver::class.java)

    override fun isBlockerServiceEnabled(): Boolean {
        val setting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        return EnabledServicesParser.isEnabled(
            setting,
            packageName = context.packageName,
            className = FocusAccessibilityService::class.java.name,
        )
    }

    /** The only DevicePolicyManager read: plain "active admin" status, no policies. */
    override fun isDeviceAdminActive(): Boolean = dpm.isAdminActive(adminComponent)
}
