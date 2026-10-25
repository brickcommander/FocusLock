package dev.focuslock.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

data class AppEntry(val packageName: String, val label: String)

interface AppSource {
    /** Apps with a launcher icon, sorted by label. */
    fun launchableApps(): List<AppEntry>

    fun label(packageName: String): String?

    /** Launcher, dialer and system UI: blocking any of them could leave the phone unusable. */
    fun protectedPackages(): Set<String>
}

/** Package lookups. Visibility comes from the <queries> intents in the manifest only. */
class InstalledApps(context: Context) : AppSource {

    private val pm: PackageManager = context.packageManager

    override fun launchableApps(): List<AppEntry> =
        query(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))
            .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }

    override fun label(packageName: String): String? =
        try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    override fun protectedPackages(): Set<String> = buildSet {
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
