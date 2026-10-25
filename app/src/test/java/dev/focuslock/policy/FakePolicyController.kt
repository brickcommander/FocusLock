package dev.focuslock.policy

class FakePolicyController : PolicyController {

    var deviceOwner: Boolean = false
    val installedPackages: MutableSet<String> = mutableSetOf()
    val calls: MutableList<String> = mutableListOf()

    /** Packages that DPM will "fail" to suspend — returned by setAppsSuspended. */
    var suspendFailures: Set<String> = emptySet()

    /** Any recorded call starting with this prefix throws, like DPM does when the admin is missing. */
    var failingCallPrefix: String? = null

    val suspendedPackages: MutableSet<String> = mutableSetOf()
    val uninstallBlockedPackages: MutableSet<String> = mutableSetOf()

    private fun record(call: String) {
        calls.add(call)
        failingCallPrefix?.let { prefix ->
            if (call.startsWith(prefix)) throw SecurityException("Fake DPM failure on $call")
        }
    }

    override fun isDeviceOwner(): Boolean {
        record("isDeviceOwner")
        return deviceOwner
    }

    override fun clearDeviceOwner() {
        record("clearDeviceOwner")
        deviceOwner = false
    }

    override fun isInstalled(pkg: String): Boolean {
        record("isInstalled:$pkg")
        return pkg in installedPackages
    }

    override fun setAppsSuspended(pkgs: List<String>, suspended: Boolean): List<String> {
        record("setAppsSuspended:suspended=$suspended:${pkgs.joinToString(",")}")
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
        record("setUserRestriction:$key=$enabled")
        if (enabled) userRestrictions.add(key) else userRestrictions.remove(key)
    }

    override fun setSelfControlDisabled(disabled: Boolean) {
        record("setSelfControlDisabled:$disabled")
        controlDisabledForSelf = disabled
    }

    val browserPolicies: MutableMap<String, BrowserPolicy?> = mutableMapOf()

    override fun setBrowserPolicy(pkg: String, policy: BrowserPolicy?) {
        record("setBrowserPolicy:$pkg=${if (policy == null) "null" else "set"}")
        browserPolicies[pkg] = policy
    }

    override fun setUninstallBlocked(pkg: String, blocked: Boolean) {
        record("setUninstallBlocked:$pkg=$blocked")
        if (blocked) {
            uninstallBlockedPackages.add(pkg)
        } else {
            uninstallBlockedPackages.remove(pkg)
        }
    }
}
