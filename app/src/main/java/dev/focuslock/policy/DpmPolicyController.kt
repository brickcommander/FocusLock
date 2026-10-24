package dev.focuslock.policy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import dev.focuslock.admin.FocusAdminReceiver

class DpmPolicyController(
    private val context: Context,
    private val schemaReader: RestrictionSchemaReader,
) : PolicyController {

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val admin = ComponentName(context, FocusAdminReceiver::class.java)

    override fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    @Suppress("DEPRECATION")
    override fun clearDeviceOwner() {
        dpm.clearDeviceOwnerApp(context.packageName)
    }

    override fun isInstalled(pkg: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun setAppsSuspended(pkgs: List<String>, suspended: Boolean): List<String> {
        if (pkgs.isEmpty()) return emptyList()
        return dpm.setPackagesSuspended(admin, pkgs.toTypedArray(), suspended).toList()
    }

    override fun setUninstallBlocked(pkg: String, blocked: Boolean) {
        dpm.setUninstallBlocked(admin, pkg, blocked)
    }

    override fun setBrowserPolicy(pkg: String, policy: BrowserPolicy?) {
        val bundle = if (policy == null) {
            Bundle()
        } else {
            RestrictionBundleMapper.toBundle(policy, schemaReader.read(pkg))
        }
        dpm.setApplicationRestrictions(admin, pkg, bundle)
    }
}
