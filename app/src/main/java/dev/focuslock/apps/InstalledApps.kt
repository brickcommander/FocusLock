package dev.focuslock.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/** Package lookups. Visibility comes from the <queries> intents in the manifest only. */
class InstalledApps(context: Context) {

    private val pm: PackageManager = context.packageManager

    /** Launcher, dialer and system UI: blocking any of them could leave the phone unusable. */
    fun protectedPackages(): Set<String> = buildSet {
        add("android")
        add("com.android.systemui")
        query(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)).forEach { add(it.activityInfo.packageName) }
        query(Intent(Intent.ACTION_DIAL)).forEach { add(it.activityInfo.packageName) }
    }

    private fun query(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
}
