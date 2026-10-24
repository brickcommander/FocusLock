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

    override fun setUninstallBlocked(pkg: String, blocked: Boolean) {
        calls.add("setUninstallBlocked:$pkg=$blocked")
        if (blocked) {
            uninstallBlockedPackages.add(pkg)
        } else {
            uninstallBlockedPackages.remove(pkg)
        }
    }
}
