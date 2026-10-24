package dev.focuslock.policy

interface PolicyController {
    fun isDeviceOwner(): Boolean
    fun clearDeviceOwner()
    fun isInstalled(pkg: String): Boolean
    fun setAppsSuspended(pkgs: List<String>, suspended: Boolean): List<String>
    fun setUninstallBlocked(pkg: String, blocked: Boolean)
    fun setBrowserPolicy(pkg: String, policy: BrowserPolicy?)   // null = clear (empty Bundle)
}
