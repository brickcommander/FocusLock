package dev.focuslock.setup

interface SetupChecker {
    fun isBlockerServiceEnabled(): Boolean
    fun isDeviceAdminActive(): Boolean

    /** Gives up our own device admin (an admin may remove itself), so the app can be uninstalled. */
    fun deactivateAdmin()
}
