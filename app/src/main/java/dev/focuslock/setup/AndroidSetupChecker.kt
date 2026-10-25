package dev.focuslock.setup

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import dev.focuslock.admin.FocusAdminReceiver
import dev.focuslock.blocker.FocusAccessibilityService

class AndroidSetupChecker(private val context: Context) : SetupChecker {

    /** Null on devices without FEATURE_DEVICE_ADMIN. */
    private val dpm: DevicePolicyManager? = context.getSystemService(DevicePolicyManager::class.java)

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

    /** The only DevicePolicyManager calls: "active admin" status and self-removal, no policies. */
    override fun isDeviceAdminActive(): Boolean = dpm?.isAdminActive(adminComponent) == true

    /** Best effort: if it fails, the uninstall screen still lets the user deactivate the admin. */
    override fun deactivateAdmin() {
        if (!isDeviceAdminActive()) return
        try {
            dpm?.removeActiveAdmin(adminComponent)
        } catch (_: SecurityException) {
        }
    }
}
