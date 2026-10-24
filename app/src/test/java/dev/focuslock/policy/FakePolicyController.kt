package dev.focuslock.policy

class FakePolicyController : PolicyController {

    var deviceOwner: Boolean = false
    val installedPackages: MutableSet<String> = mutableSetOf()
    val calls: MutableList<String> = mutableListOf()

    /** Packages that DPM will "fail" to suspend — returned by setAppsSuspended. */
    var suspendFailures: Set<String> = emptySet()

    val suspendedPackages: MutableSet<String> = mutableSetOf()
    val uninstallBlockedPackages: MutableSet<String> = mutableSetOf()

    override fun isDeviceOwner(): Boolean {
        calls.add("isDeviceOwner")
        return deviceOwner
    }

    override fun clearDeviceOwner() {
        calls.add("clearDeviceOwner")
        deviceOwner = false
    }

    override fun isInstalled(pkg: String): Boolean {
        calls.add("isInstalled:$pkg")
        return pkg in installedPackages
    }

    override fun setAppsSuspended(pkgs: List<String>, suspended: Boolean): List<String> {
        calls.add("setAppsSuspended:suspended=$suspended:${pkgs.joinToString(",")}")
        val failed = pkgs.filter { it in suspendFailures }
        val succeeded = pkgs.filter { it !in suspendFailures }
        if (suspended) {
            suspendedPackages.addAll(succeeded)
        } else {
            suspendedPackages.removeAll(succeeded.toSet())
        }
        return failed
    }

    val userRestrictions: MutableSet<String> = mutableSetOf()
    var controlDisabledForSelf: Boolean = false

    override fun setUserRestriction(key: String, enabled: Boolean) {
        calls.add("setUserRestriction:$key=$enabled")
        if (enabled) userRestrictions.add(key) else userRestrictions.remove(key)
    }

    override fun setSelfControlDisabled(disabled: Boolean) {
        calls.add("setSelfControlDisabled:$disabled")
        controlDisabledForSelf = disabled
    }

    val browserPolicies: MutableMap<String, BrowserPolicy?> = mutableMapOf()

    override fun setBrowserPolicy(pkg: String, policy: BrowserPolicy?) {
        calls.add("setBrowserPolicy:$pkg=${if (policy == null) "null" else "set"}")
        browserPolicies[pkg] = policy
    }

    override fun setUninstallBlocked(pkg: String, blocked: Boolean) {
        calls.add("setUninstallBlocked:$pkg=$blocked")
        if (blocked) {
            uninstallBlockedPackages.add(pkg)
        } else {
            uninstallBlockedPackages.remove(pkg)
        }
    }
}
