package dev.focuslock.setup

interface SetupChecker {
    fun isBlockerServiceEnabled(): Boolean
    fun isDeviceAdminActive(): Boolean
}
